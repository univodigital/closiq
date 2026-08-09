package com.closiq.storage;

/**
 * Supported object-storage backends. Business logic must depend on {@link FileStorageService},
 * not on a specific provider.
 */
public enum StorageProvider {
    CLOUDINARY,
    S3
}
