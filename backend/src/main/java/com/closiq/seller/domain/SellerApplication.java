package com.closiq.seller.domain;

import com.closiq.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_application")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerApplication extends AuditableEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private com.closiq.identity.domain.User user;

    @Column(name = "business_name", nullable = false, length = 100)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 30)
    private BusinessType businessType;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(length = 500)
    private String description;

    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    @Column(name = "pan_number", nullable = false, length = 10)
    private String panNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
