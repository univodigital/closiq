package com.closiq.booking.repository;

import com.closiq.booking.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    Optional<Booking> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Booking> findByRentalNumberAndCustomerId(String rentalNumber, UUID customerId);

    Optional<Booking> findByRentalNumber(String rentalNumber);

    Optional<Booking> findByOrderNumber(String orderNumber);

    Optional<Booking> findByOrderNumberAndCustomerId(String orderNumber, UUID customerId);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.customerId = :customerId
            AND (:status IS NULL OR b.status = :status)
            AND (b.createdAt < :beforeCreatedAt
                 OR (b.createdAt = :beforeCreatedAt AND b.id < :beforeId))
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    List<Booking> findCustomerPage(
            @Param("customerId") UUID customerId,
            @Param("status") String status,
            @Param("beforeCreatedAt") Instant beforeCreatedAt,
            @Param("beforeId") UUID beforeId,
            org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = 'PENDING_PAYMENT'
            AND b.holdExpiresAt IS NOT NULL
            AND b.holdExpiresAt < :now
            """)
    List<Booking> findExpiredPendingPayment(@Param("now") Instant now);

    long countBySellerProfileIdAndStatus(UUID sellerProfileId, String status);
}
