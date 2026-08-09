package com.closiq.booking.repository;

import com.closiq.booking.domain.CheckoutSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, UUID> {

    java.util.Optional<CheckoutSession> findByIdAndCustomerId(UUID id, UUID customerId);
}
