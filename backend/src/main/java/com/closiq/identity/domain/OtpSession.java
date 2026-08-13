package com.closiq.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSession {

    @Id
    private UUID id;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    @Column(nullable = false)
    private short attempts;

    @Column(name = "resend_count", nullable = false)
    private short resendCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpSessionStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "delivery_email", length = 255)
    private String deliveryEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
