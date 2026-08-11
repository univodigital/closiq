package com.closiq.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkout_batch")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutBatch {

    public static final String OPEN = "OPEN";
    public static final String COMPLETED = "COMPLETED";
    public static final String EXPIRED = "EXPIRED";
    public static final String FAILED = "FAILED";

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "delivery_address_id")
    private UUID deliveryAddressId;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
