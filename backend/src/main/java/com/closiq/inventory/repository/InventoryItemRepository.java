package com.closiq.inventory.repository;

import com.closiq.inventory.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    List<InventoryItem> findByProductVariantIdAndStatusNotOrderByCreatedAtAsc(
            UUID productVariantId, String status);

    List<InventoryItem> findByProductVariantIdAndStatusOrderByCreatedAtAsc(
            UUID productVariantId, String status);

    long countByProductVariantIdAndStatus(UUID productVariantId, String status);

    @Query("""
            SELECT i FROM InventoryItem i
            WHERE i.productVariant.id IN :variantIds
            AND i.status <> :retiredStatus
            """)
    List<InventoryItem> findRentableByVariantIds(
            @Param("variantIds") Collection<UUID> variantIds,
            @Param("retiredStatus") String retiredStatus);

    @Query("""
            SELECT i.productVariant.id, COUNT(i) FROM InventoryItem i
            WHERE i.productVariant.id IN :variantIds
            AND i.status = :availableStatus
            GROUP BY i.productVariant.id
            """)
    List<Object[]> countAvailableByVariantIds(
            @Param("variantIds") Collection<UUID> variantIds,
            @Param("availableStatus") String availableStatus);

    @Query("""
            SELECT i.productVariant.product.id, COUNT(i) FROM InventoryItem i
            WHERE i.productVariant.product.id IN :productIds
            AND i.status = :availableStatus
            GROUP BY i.productVariant.product.id
            """)
    List<Object[]> countAvailableByProductIds(
            @Param("productIds") Collection<UUID> productIds,
            @Param("availableStatus") String availableStatus);
}
