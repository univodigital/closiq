package com.closiq.identity.repository;

import com.closiq.identity.domain.RefreshToken;
import com.closiq.identity.domain.RefreshTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndStatus(String tokenHash, RefreshTokenStatus status);

    @Modifying
    @Transactional
    @Query("""
            UPDATE RefreshToken rt SET rt.status = :revokedStatus, rt.revokedAt = :now
            WHERE rt.user.id = :userId AND rt.status = :activeStatus
            """)
    int revokeAllActiveForUser(
            @Param("userId") UUID userId,
            @Param("activeStatus") RefreshTokenStatus activeStatus,
            @Param("revokedStatus") RefreshTokenStatus revokedStatus,
            @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("""
            UPDATE RefreshToken rt SET rt.status = :revokedStatus, rt.revokedAt = :now
            WHERE rt.familyId = :familyId AND rt.status = :activeStatus
            """)
    int revokeFamily(
            @Param("familyId") UUID familyId,
            @Param("activeStatus") RefreshTokenStatus activeStatus,
            @Param("revokedStatus") RefreshTokenStatus revokedStatus,
            @Param("now") Instant now);
}
