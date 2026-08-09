package com.closiq.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "booking")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    private UUID id;

    @Column(name = "rental_number", nullable = false, unique = true, length = 32)
    private String rentalNumber;

    @Column(name = "order_number", unique = true, length = 32)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "seller_profile_id")
    private UUID sellerProfileId;

    @Column(name = "delivery_address_id")
    private UUID deliveryAddressId;

    @Column(name = "checkout_session_id")
    private UUID checkoutSessionId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "rental_start_date", nullable = false)
    private LocalDate rentalStartDate;

    @Column(name = "rental_end_date", nullable = false)
    private LocalDate rentalEndDate;

    @Column(name = "rental_days", nullable = false)
    private short rentalDays;

    @Column(name = "rental_amount", nullable = false)
    private long rentalAmount;

    @Column(name = "deposit_amount", nullable = false)
    private long depositAmount;

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount;

    @Column(name = "delivery_fee", nullable = false)
    private long deliveryFee;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "includes_trial", nullable = false)
    private boolean includesTrial;

    @Column(name = "trial_duration_minutes", nullable = false)
    private short trialDurationMinutes;

    @Column(name = "customer_notes", length = 200)
    private String customerNotes;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 50)
    private String cancelReason;

    @Column(name = "cancel_comment", columnDefinition = "TEXT")
    private String cancelComment;

    @Column(name = "seller_prep_by")
    private Instant sellerPrepBy;

    @Column(name = "seller_notes", columnDefinition = "TEXT")
    private String sellerNotes;

    @Column(name = "seller_accepted_at")
    private Instant sellerAcceptedAt;

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
