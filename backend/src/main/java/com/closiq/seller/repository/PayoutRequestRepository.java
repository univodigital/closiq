package com.closiq.seller.repository;

import com.closiq.seller.domain.PayoutRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, UUID> {

    Optional<PayoutRequest> findByIdempotencyKey(String idempotencyKey);
}
