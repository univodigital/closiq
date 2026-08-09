package com.closiq.user.domain;

import com.closiq.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "address")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address extends AuditableEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private com.closiq.identity.domain.User user;

    @Column(nullable = false, length = 20)
    private String label;

    @Column(nullable = false, length = 100)
    private String line1;

    @Column(length = 100)
    private String line2;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false, length = 50)
    private String state;

    @Column(nullable = false, length = 6)
    private String pincode;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
