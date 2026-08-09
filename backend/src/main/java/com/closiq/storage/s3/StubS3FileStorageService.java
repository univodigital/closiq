package com.closiq.storage.s3;

import com.closiq.config.ClosiqProperties;
import com.closiq.storage.FileStorageService;
import com.closiq.storage.StorageProvider;
import com.closiq.storage.UploadInstruction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Stub S3 implementation of {@link FileStorageService}.
 * Replace with real AWS SDK presigning when migrating off Cloudinary.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "closiq.storage.provider", havingValue = "s3")
public class StubS3FileStorageService implements FileStorageService {

    private final ClosiqProperties properties;

    @Override
    public StorageProvider provider() {
        return StorageProvider.S3;
    }

    @Override
    public String buildStorageKey(String relativePath) {
        return relativePath;
    }

    @Override
    public String storageNamespace() {
        return properties.getS3().getBucket();
    }

    @Override
    public String resolvePublicUrl(String storageKey, String contentType) {
        return properties.getS3().getCdnBaseUrl() + "/" + storageKey;
    }

    @Override
    public UploadInstruction createUploadInstruction(String relativePath, String contentType) {
        String storageKey = buildStorageKey(relativePath);
        String fileUrl = resolvePublicUrl(storageKey, contentType);
        ClosiqProperties.S3 s3 = properties.getS3();

        String uploadUrl = s3.isStubEnabled()
                ? "https://" + s3.getBucket() + ".s3." + s3.getRegion() + ".amazonaws.com/" + storageKey
                : fileUrl;

        return UploadInstruction.builder()
                .storageKey(storageKey)
                .fileUrl(fileUrl)
                .uploadUrl(uploadUrl)
                .method("PUT")
                .headers(Map.of("Content-Type", contentType))
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();
    }

    @Override
    public void delete(String storageKey) {
        if (properties.getS3().isStubEnabled()) {
            log.debug("S3 stub: skipping delete for storageKey={}", storageKey);
            return;
        }
        log.info("S3 delete requested for storageKey={} (wire AWS SDK when provider=s3 is production-ready)", storageKey);
    }
}
