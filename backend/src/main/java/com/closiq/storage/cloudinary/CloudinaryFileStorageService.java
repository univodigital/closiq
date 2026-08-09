package com.closiq.storage.cloudinary;

import com.closiq.config.ClosiqProperties;
import com.closiq.storage.FileStorageService;
import com.closiq.storage.StorageProvider;
import com.closiq.storage.UploadInstruction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    private final ClosiqProperties properties;

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
                + "/upload/v1/"
                + storageKey;
    }

    @Override
    public UploadInstruction createUploadInstruction(String relativePath, String contentType) {
        String storageKey = buildStorageKey(relativePath);
        String fileUrl = resolvePublicUrl(storageKey, contentType);

        if (properties.getCloudinary().isStubEnabled() || !hasCredentials()) {
            return UploadInstruction.builder()
                    .storageKey(storageKey)
                    .fileUrl(fileUrl)
                    .uploadUrl(fileUrl)
                    .method("POST")
                    .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                    .build();
        }

        ClosiqProperties.Cloudinary cloudinary = properties.getCloudinary();
        long timestamp = Instant.now().getEpochSecond();
        String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudinary.getCloudName() + "/auto/upload";

        Map<String, String> signParams = new TreeMap<>();
        signParams.put("folder", cloudinary.getFolder());
        signParams.put("public_id", relativePath);
        signParams.put("timestamp", Long.toString(timestamp));

        String signature = sign(signParams, cloudinary.getApiSecret());

        Map<String, String> formFields = new LinkedHashMap<>();
        formFields.put("api_key", cloudinary.getApiKey());
        formFields.put("timestamp", Long.toString(timestamp));
        formFields.put("signature", signature);
        formFields.put("folder", cloudinary.getFolder());
        formFields.put("public_id", relativePath);

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
    public void delete(String storageKey) {
        if (properties.getCloudinary().isStubEnabled() || !hasCredentials()) {
            log.debug("Cloudinary stub: skipping delete for storageKey={}", storageKey);
            return;
        }
        // Cloudinary Admin API destroy can be wired here during provider migration tooling.
        log.info("Cloudinary delete requested for storageKey={} (implement destroy API when needed)", storageKey);
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

    private static String sign(Map<String, String> params, String apiSecret) {
        String toSign = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest((toSign + apiSecret).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }
}
