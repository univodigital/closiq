package com.closiq.storage;

import com.closiq.common.util.IdGenerator;
import com.closiq.identity.domain.User;
import com.closiq.seller.domain.MediaAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Creates {@link MediaAsset} rows using provider-neutral storage metadata.
 */
@Component
@RequiredArgsConstructor
public class MediaAssetFactory {

    private final FileStorageService fileStorageService;

    public MediaAsset createPendingUpload(
            UUID uploadId,
            User uploadedBy,
            String relativePath,
            String originalFilename,
            String mimeType) {

        return MediaAsset.builder()
                .id(uploadId)
                .uploadedBy(uploadedBy)
                .storageProvider(fileStorageService.provider())
                .storageBucket(fileStorageService.storageNamespace())
                .storageKey(fileStorageService.buildStorageKey(relativePath))
                .originalFilename(originalFilename)
                .mimeType(mimeType)
                .status("UPLOADED")
                .createdAt(Instant.now())
                .build();
    }

    public MediaAsset createPendingUpload(User uploadedBy, String relativePath, String originalFilename, String mimeType) {
        return createPendingUpload(IdGenerator.uuidV7(), uploadedBy, relativePath, originalFilename, mimeType);
    }
}
