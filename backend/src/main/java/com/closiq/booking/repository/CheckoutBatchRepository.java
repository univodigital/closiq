package com.closiq.booking.repository;

import com.closiq.booking.domain.CheckoutBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckoutBatchRepository extends JpaRepository<CheckoutBatch, UUID> {

    Optional<CheckoutBatch> findByIdempotencyKey(String idempotencyKey);

    Optional<CheckoutBatch> findByIdAndCustomerId(UUID id, UUID customerId);

    List<CheckoutBatch> findByCustomerIdAndStatusOrderByCreatedAtDesc(UUID customerId, String status);
}
