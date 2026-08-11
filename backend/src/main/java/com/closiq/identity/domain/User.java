package com.closiq.identity.domain;

import com.closiq.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"user\"")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "user_code", nullable = false, unique = true, length = 32)
    private String userCode;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(length = 255)
    private String email;

    @Column(name = "pending_email", length = 255)
    private String pendingEmail;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "alternate_phone", length = 15)
    private String alternatePhone;

    @Column(name = "alternate_email", length = 255)
    private String alternateEmail;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
