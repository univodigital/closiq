package com.closiq.inventory.service;

import com.closiq.config.ClosiqProperties;
import com.closiq.inventory.domain.InventoryItemStatus;
import com.closiq.inventory.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryStockService {

    private final InventoryItemRepository inventoryItemRepository;
    private final ClosiqProperties properties;

    @Transactional(readOnly = true)
    public int countAvailableUnits(UUID variantId) {
        return (int) inventoryItemRepository.countByProductVariantIdAndStatus(
                variantId, InventoryItemStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public Map<UUID, Integer> countAvailableUnitsByVariant(Collection<UUID> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Integer> counts = new HashMap<>();
        List<Object[]> rows = inventoryItemRepository.countAvailableByVariantIds(
                variantIds, InventoryItemStatus.AVAILABLE);
        for (Object[] row : rows) {
            counts.put((UUID) row[0], ((Long) row[1]).intValue());
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public Map<UUID, Integer> countAvailableUnitsByProduct(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Integer> counts = new HashMap<>();
        List<Object[]> rows = inventoryItemRepository.countAvailableByProductIds(
                productIds, InventoryItemStatus.AVAILABLE);
        for (Object[] row : rows) {
            counts.put((UUID) row[0], ((Long) row[1]).intValue());
        }
        return counts;
    }

    public boolean isLowStock(int totalAvailableUnits) {
        return totalAvailableUnits > 0
                && totalAvailableUnits <= properties.getInventory().getLowStockThreshold();
    }
}
