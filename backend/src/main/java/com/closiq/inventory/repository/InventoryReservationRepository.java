package com.closiq.inventory.repository;

import com.closiq.inventory.domain.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

    @Query("""
            SELECT r FROM InventoryReservation r
            JOIN FETCH r.inventoryItem i
            WHERE i.productVariant.id = :variantId
            AND r.status = 'ACTIVE'
            AND r.endDate >= :rangeStart
            AND r.startDate <= :rangeEnd
            """)
    List<InventoryReservation> findActiveForVariantInRange(
            @Param("variantId") UUID variantId,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd);

    @Query("""
            SELECT COUNT(r) FROM InventoryReservation r
            JOIN r.inventoryItem i
            WHERE i.productVariant.id = :variantId
            AND r.status = 'ACTIVE'
            AND r.reservationType IN ('HOLD', 'CONFIRMED')
            """)
    long countActiveBookingsForVariant(@Param("variantId") UUID variantId);

    List<InventoryReservation> findByBookingIdAndStatus(UUID bookingId, String status);

    @Query("""
            SELECT r FROM InventoryReservation r
            WHERE r.status = 'ACTIVE'
            AND r.reservationType = 'HOLD'
            AND r.holdExpiresAt IS NOT NULL
            AND r.holdExpiresAt < :now
            """)
    List<InventoryReservation> findExpiredHolds(@Param("now") Instant now);
}
