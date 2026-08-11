package com.closiq.payment.repository;

import com.closiq.payment.domain.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findByPaymentIdOrderByInitiatedAtAsc(UUID paymentId);

    List<Refund> findByBookingIdOrderByInitiatedAtAsc(UUID bookingId);

    Optional<Refund> findByIdempotencyKey(String idempotencyKey);
}
