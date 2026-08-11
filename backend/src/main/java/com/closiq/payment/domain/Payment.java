package com.closiq.payment.domain;

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
@Table(name = "payment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "checkout_session_id")
    private UUID checkoutSessionId;

    @Column(name = "checkout_batch_id")
    private UUID checkoutBatchId;

    @Column(name = "provider_code", nullable = false, length = 20)
    private String providerCode;

    @Column(name = "provider_order_id", nullable = false, unique = true, length = 100)
    private String providerOrderId;

    @Column(name = "provider_payment_id", unique = true, length = 100)
    private String providerPaymentId;

    /** Amount in paise (INR smallest unit). */
    @Column(nullable = false)
    private long amount;

    @Column(name = "rental_component", nullable = false)
    private long rentalComponent;

    @Column(name = "deposit_component", nullable = false)
    private long depositComponent;

    @Column(name = "discount_component", nullable = false)
    private long discountComponent;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

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
