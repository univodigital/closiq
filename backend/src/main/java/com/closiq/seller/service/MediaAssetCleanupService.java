package com.closiq.seller.service;

import com.closiq.seller.domain.MediaAsset;
import com.closiq.seller.repository.MediaAssetRepository;
import com.closiq.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaAssetCleanupService {

    private final MediaAssetRepository mediaAssetRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public void orphanAndDelete(MediaAsset asset) {
        if (asset == null) {
            return;
        }
        if ("ATTACHED".equals(asset.getStatus())) {
            return;
        }
        asset.setStatus("ORPHANED");
        mediaAssetRepository.save(asset);
        deleteFromStorage(asset);
    }

    private void deleteFromStorage(MediaAsset asset) {
        try {
            fileStorageService.delete(asset.getStorageKey());
        } catch (Exception ex) {
            log.warn("Failed to delete storage object {}: {}", asset.getStorageKey(), ex.getMessage());
        }
    }
}
