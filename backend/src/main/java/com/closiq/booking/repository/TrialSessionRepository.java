package com.closiq.booking.repository;

import com.closiq.booking.domain.TrialSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrialSessionRepository extends JpaRepository<TrialSession, UUID> {

    Optional<TrialSession> findByBookingId(UUID bookingId);
}
