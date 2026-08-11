package com.closiq.booking.repository;

import com.closiq.booking.domain.TrialSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TrialSessionRepository extends JpaRepository<TrialSession, UUID> {

    Optional<TrialSession> findByBookingId(UUID bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TrialSession t WHERE t.bookingId = :bookingId")
    Optional<TrialSession> findByBookingIdForUpdate(@Param("bookingId") UUID bookingId);
}
