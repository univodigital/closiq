package com.closiq.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "refund")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "initiated_by")
    private UUID initiatedBy;

    @Column(name = "refund_type", nullable = false, length = 20)
    private String refundType;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "provider_refund_id", length = 100)
    private String providerRefundId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "expected_by")
    private Instant expectedBy;
}
