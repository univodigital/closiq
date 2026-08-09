package com.closiq.user.domain;

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
@Table(name = "seller_profile")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerProfile {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private com.closiq.identity.domain.User user;

    @Column(name = "business_name", nullable = false, length = 100)
    private String businessName;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "avg_rating", precision = 2, scale = 1)
    private BigDecimal avgRating;

    @Column(length = 50)
    private String city;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
