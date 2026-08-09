package com.closiq.identity.repository;

import com.closiq.identity.domain.OtpSession;
import com.closiq.identity.domain.OtpSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpSessionRepository extends JpaRepository<OtpSession, UUID> {

    Optional<OtpSession> findByIdAndStatus(UUID id, OtpSessionStatus status);
}
