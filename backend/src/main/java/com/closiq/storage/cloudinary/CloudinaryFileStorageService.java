package com.closiq.storage.cloudinary;

import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.config.ClosiqProperties;
import com.closiq.storage.FileStorageService;
import com.closiq.storage.StorageProvider;
import com.closiq.storage.StoredUploadResult;
import com.closiq.storage.UploadInstruction;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Cloudinary implementation of {@link FileStorageService}.
 * All Cloudinary-specific signing, URL structure, and resource types stay in this class.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "closiq.storage.provider", havingValue = "cloudinary", matchIfMissing = true)
public class CloudinaryFileStorageService implements FileStorageService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final ClosiqProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public StorageProvider provider() {
        return StorageProvider.CLOUDINARY;
    }

    @Override
    public String buildStorageKey(String relativePath) {
        return properties.getCloudinary().getFolder() + "/" + relativePath;
    }

    @Override
    public String storageNamespace() {
        return properties.getCloudinary().getCloudName();
    }

    @Override
    public String resolvePublicUrl(String storageKey, String contentType) {
        String resourceType = isVideo(contentType) ? "video" : "image";
        return "https://res.cloudinary.com/"
                + properties.getCloudinary().getCloudName()
                + "/"
                + resourceType
                + "/upload/"
                + storageKey;
    }

    @Override
    public UploadInstruction createUploadInstruction(String relativePath, String contentType) {
        String storageKey = buildStorageKey(relativePath);
        String fileUrl = resolvePublicUrl(storageKey, contentType);

        if (properties.getCloudinary().isStubEnabled()) {
            return UploadInstruction.builder()
                    .storageKey(storageKey)
                    .fileUrl(fileUrl)
                    .uploadUrl(fileUrl)
                    .method("POST")
                    .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                    .build();
        }

        requireSignedUploadCredentials();

        ClosiqProperties.Cloudinary cloudinary = properties.getCloudinary();
        long timestamp = Instant.now().getEpochSecond();
        String resourceType = isVideo(contentType) ? "video" : "image";
        String uploadUrl = "https://api.cloudinary.com/v1_1/"
                + cloudinary.getCloudName()
                + "/"
                + resourceType
                + "/upload";

        // Use the full storage key as public_id (no separate folder param) so the signed
        // parameters match exactly what Cloudinary validates.
        Map<String, String> signParams = new TreeMap<>();
        signParams.put("public_id", storageKey);
        signParams.put("timestamp", Long.toString(timestamp));

        String signature = signUploadParams(signParams, cloudinary.getApiSecret());

        Map<String, String> formFields = new LinkedHashMap<>();
        formFields.put("api_key", cloudinary.getApiKey());
        formFields.put("timestamp", Long.toString(timestamp));
        formFields.put("signature", signature);
        formFields.put("public_id", storageKey);

        return UploadInstruction.builder()
                .storageKey(storageKey)
                .fileUrl(fileUrl)
                .uploadUrl(uploadUrl)
                .method("POST")
                .formFields(formFields)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();
    }

    @Override
    public StoredUploadResult uploadBytes(String relativePath, String contentType, String filename, byte[] bytes) {
        CloudinaryPath path = CloudinaryPath.fromRelativePath(relativePath, properties.getCloudinary().getFolder());
        String storageKey = path.storageKey();
        String fileUrl = resolvePublicUrl(storageKey, contentType);

        if (properties.getCloudinary().isStubEnabled()) {
            return StoredUploadResult.builder().storageKey(storageKey).publicUrl(fileUrl).build();
        }

        requireSignedUploadCredentials();

        ClosiqProperties.Cloudinary cloudinary = properties.getCloudinary();
        String resourceType = isVideo(contentType) ? "video" : "image";
        String uploadUrl = "https://api.cloudinary.com/v1_1/"
                + cloudinary.getCloudName()
                + "/"
                + resourceType
                + "/upload";

        try {
            byte[] body = buildMultipartBody(path, filename, contentType, bytes);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Authorization", basicAuthHeader(cloudinary))
                    .header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Cloudinary upload failed status={} body={}", response.statusCode(), response.body());
                throw new ClosiqException(
                        ErrorCode.INTERNAL_ERROR,
                        parseCloudinaryErrorMessage(response.body(), response.statusCode()));
            }

            CloudinaryUploadResponse payload =
                    objectMapper.readValue(response.body(), CloudinaryUploadResponse.class);
            String publicUrl = payload.secureUrl() != null ? payload.secureUrl() : fileUrl;
            return StoredUploadResult.builder().storageKey(storageKey).publicUrl(publicUrl).build();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ClosiqException(ErrorCode.INTERNAL_ERROR, "Cloudinary upload interrupted");
        } catch (IOException ex) {
            throw new ClosiqException(ErrorCode.INTERNAL_ERROR, "Cloudinary upload failed");
        }
    }

    private static final String MULTIPART_BOUNDARY = "----ClosiqCloudinaryUpload";

    @Override
    public void delete(String storageKey) {
        if (properties.getCloudinary().isStubEnabled() || !hasCredentials()) {
            log.debug("Cloudinary stub: skipping delete for storageKey={}", storageKey);
            return;
        }

        ClosiqProperties.Cloudinary cloudinary = properties.getCloudinary();
        CloudinaryPath path = CloudinaryPath.fromStorageKey(storageKey, cloudinary.getFolder());
        long timestamp = Instant.now().getEpochSecond();

        Map<String, String> signParams = new TreeMap<>();
        signParams.put("public_id", path.publicId());
        signParams.put("timestamp", Long.toString(timestamp));
        if (path.folder() != null) {
            signParams.put("folder", path.folder());
        }
        String signature = signUploadParams(signParams, cloudinary.getApiSecret());

        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("public_id=").append(urlEncode(path.publicId()));
        if (path.folder() != null) {
            bodyBuilder.append("&folder=").append(urlEncode(path.folder()));
        }
        bodyBuilder
                .append("&timestamp=")
                .append(timestamp)
                .append("&api_key=")
                .append(urlEncode(cloudinary.getApiKey()))
                .append("&signature=")
                .append(signature);
        String body = bodyBuilder.toString();

        String destroyUrl = "https://api.cloudinary.com/v1_1/"
                + cloudinary.getCloudName()
                + "/image/destroy";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(destroyUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn(
                        "Cloudinary destroy failed for publicId={} status={} body={}",
                        storageKey,
                        response.statusCode(),
                        response.body());
            }
        } catch (Exception ex) {
            log.warn("Cloudinary destroy failed for publicId={}: {}", storageKey, ex.getMessage());
        }
    }

    void requireSignedUploadCredentials() {
        if (properties.getCloudinary().isStubEnabled()) {
            return;
        }
        if (!hasCredentials()) {
            throw new ClosiqException(
                    ErrorCode.INTERNAL_ERROR,
                    "Cloudinary uploads require CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and "
                            + "CLOUDINARY_API_SECRET from the same Cloudinary account, or set "
                            + "CLOUDINARY_STUB_ENABLED=true for local development");
        }
    }

    private boolean hasCredentials() {
        ClosiqProperties.Cloudinary cloudinary = properties.getCloudinary();
        return cloudinary.getCloudName() != null
                && !cloudinary.getCloudName().isBlank()
                && cloudinary.getApiKey() != null
                && !cloudinary.getApiKey().isBlank()
                && cloudinary.getApiSecret() != null
                && !cloudinary.getApiSecret().isBlank();
    }

    private static boolean isVideo(String contentType) {
        return contentType != null && contentType.startsWith("video/");
    }

    static String signUploadParams(Map<String, String> params, String apiSecret) {
        String normalizedSecret = apiSecret == null ? "" : apiSecret.trim();
        String toSign = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest((toSign + normalizedSecret).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String basicAuthHeader(ClosiqProperties.Cloudinary cloudinary) {
        String credentials = cloudinary.getApiKey() + ":" + cloudinary.getApiSecret();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] buildMultipartBody(
            CloudinaryPath path, String filename, String contentType, byte[] fileBytes) throws IOException {
        String safeFilename = filename == null || filename.isBlank() ? "upload.jpg" : filename;
        String safeContentType = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;

        var output = new java.io.ByteArrayOutputStream();
        writeField(output, "public_id", path.publicId());
        if (path.folder() != null) {
            writeField(output, "folder", path.folder());
        }
        output.write(("--" + MULTIPART_BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(
                ("Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFilename + "\"\r\n")
                        .getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + safeContentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(fileBytes);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        output.write(("--" + MULTIPART_BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private static void writeField(java.io.ByteArrayOutputStream output, String name, String value)
            throws IOException {
        output.write(("--" + MULTIPART_BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(
                ("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String parseCloudinaryErrorMessage(String body, int statusCode) {
        if (body == null || body.isBlank()) {
            return "Cloudinary upload failed (" + statusCode + ")";
        }
        try {
            CloudinaryErrorResponse error = new ObjectMapper().readValue(body, CloudinaryErrorResponse.class);
            if (error.error() != null && error.error().message() != null && !error.error().message().isBlank()) {
                return error.error().message();
            }
        } catch (IOException ignored) {
            // fall through
        }
        return "Cloudinary upload failed (" + statusCode + ")";
    }

    record CloudinaryPath(String folder, String publicId, String storageKey) {

        static CloudinaryPath fromRelativePath(String relativePath, String rootFolder) {
            String[] parts = relativePath.split("/", 3);
            if (parts.length == 3 && "products".equals(parts[0])) {
                String folder = rootFolder + "/products/" + parts[1];
                String publicId = parts[2];
                return new CloudinaryPath(folder, publicId, folder + "/" + publicId);
            }
            String storageKey = rootFolder + "/" + relativePath;
            return new CloudinaryPath(null, storageKey, storageKey);
        }

        static CloudinaryPath fromStorageKey(String storageKey, String rootFolder) {
            String prefix = rootFolder + "/products/";
            if (storageKey.startsWith(prefix)) {
                String remainder = storageKey.substring(prefix.length());
                int slash = remainder.indexOf('/');
                if (slash > 0 && slash < remainder.length() - 1) {
                    String productId = remainder.substring(0, slash);
                    String publicId = remainder.substring(slash + 1);
                    return new CloudinaryPath(rootFolder + "/products/" + productId, publicId, storageKey);
                }
            }
            return new CloudinaryPath(null, storageKey, storageKey);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CloudinaryUploadResponse(@JsonProperty("secure_url") String secureUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CloudinaryErrorResponse(CloudinaryErrorBody error) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CloudinaryErrorBody(String message) {}
}
