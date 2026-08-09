package com.closiq.seller.domain;

import com.closiq.storage.StorageProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_asset")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAsset {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private com.closiq.identity.domain.User uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 20)
    private StorageProvider storageProvider;

    /** Provider namespace: Cloudinary cloud name or S3 bucket (legacy column name: s3_bucket). */
    @Column(name = "s3_bucket", nullable = false)
    private String storageBucket;

    /** Logical storage key (legacy column name: s3_key). */
    @Column(name = "s3_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
