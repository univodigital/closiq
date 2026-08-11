package com.closiq.inventory.repository;

import com.closiq.inventory.domain.InventoryBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryBlockRepository extends JpaRepository<InventoryBlock, UUID> {

    @Query("""
            SELECT b FROM InventoryBlock b
            WHERE b.productVariant.id = :variantId
            AND b.status = 'ACTIVE'
            AND b.endDate >= :rangeStart
            AND b.startDate <= :rangeEnd
            """)
    List<InventoryBlock> findActiveForVariantInRange(
            @Param("variantId") UUID variantId,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd);

    @Query("""
            SELECT b FROM InventoryBlock b
            WHERE b.productVariant.id IN :variantIds
            AND b.status = 'ACTIVE'
            AND b.endDate >= :rangeStart
            AND b.startDate <= :rangeEnd
            """)
    List<InventoryBlock> findActiveForVariantsInRange(
            @Param("variantIds") Collection<UUID> variantIds,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd);

    Optional<InventoryBlock> findByIdAndCreatedByAndStatus(UUID id, UUID createdBy, String status);

    @Query("""
            SELECT b FROM InventoryBlock b
            JOIN FETCH b.productVariant v
            JOIN FETCH v.product p
            WHERE p.sellerProfileId = :sellerProfileId
            AND b.status = 'ACTIVE'
            ORDER BY b.startDate ASC, b.id ASC
            """)
    List<InventoryBlock> findActiveBySellerProfileId(@Param("sellerProfileId") UUID sellerProfileId);
}
