package com.closiq.seller.repository;

import com.closiq.seller.domain.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    Optional<MediaAsset> findByIdAndUploadedById(UUID id, UUID userId);
}
