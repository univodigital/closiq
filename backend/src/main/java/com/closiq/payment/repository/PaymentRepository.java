package com.closiq.payment.repository;

import com.closiq.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByCheckoutBatchId(UUID checkoutBatchId);

    Optional<Payment> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Payment> findByProviderOrderId(String providerOrderId);

    Optional<Payment> findByBookingIdAndStatus(UUID bookingId, String status);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.customerId = :customerId
            AND (:status IS NULL OR p.status = :status)
            AND (:bookingId IS NULL OR p.bookingId = :bookingId)
            AND (p.createdAt < :beforeCreatedAt
                 OR (p.createdAt = :beforeCreatedAt AND p.id < :beforeId))
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<Payment> findCustomerPage(
            @Param("customerId") UUID customerId,
            @Param("status") String status,
            @Param("bookingId") UUID bookingId,
            @Param("beforeCreatedAt") Instant beforeCreatedAt,
            @Param("beforeId") UUID beforeId,
            org.springframework.data.domain.Pageable pageable);
}
