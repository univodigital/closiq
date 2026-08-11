package com.closiq.storage;

import com.closiq.seller.domain.MediaAsset;

/**
 * Provider-neutral file storage abstraction.
 * <p>
 * Controllers and business services depend on this interface only.
 * Cloudinary- and S3-specific code lives in their respective implementations.
 */
public interface FileStorageService {

    StorageProvider provider();

    /**
     * Builds the full storage key from a logical relative path, e.g.
     * {@code kyc/{userId}/pan/{uploadId}} → {@code closiq/kyc/{userId}/pan/{uploadId}}.
     */
    String buildStorageKey(String relativePath);

    /** Provider namespace persisted on {@code MediaAsset} (Cloudinary cloud name or S3 bucket). */
    String storageNamespace();

    /** Resolves the public delivery URL for a stored object. */
    String resolvePublicUrl(String storageKey, String contentType);

    default String resolvePublicUrl(MediaAsset asset) {
        return resolvePublicUrl(asset.getStorageKey(), asset.getMimeType());
    }

    /** Creates client-side upload instructions for a direct upload. */
    UploadInstruction createUploadInstruction(String relativePath, String contentType);

    /** Uploads bytes from the server using provider credentials (preferred for browser uploads). */
    StoredUploadResult uploadBytes(String relativePath, String contentType, String filename, byte[] bytes);

    /** Deletes the object at {@code storageKey}. Authorization must happen before calling this. */
    void delete(String storageKey);
}
