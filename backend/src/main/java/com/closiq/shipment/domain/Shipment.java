package com.closiq.shipment.domain;

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
@Table(name = "shipment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "logistics_provider_id", nullable = false)
    private UUID logisticsProviderId;

    @Column(name = "origin_address_id")
    private UUID originAddressId;

    @Column(name = "destination_address_id")
    private UUID destinationAddressId;

    @Column(name = "shipment_type", nullable = false, length = 20)
    private String shipmentType;

    @Column(name = "provider_shipment_id", length = 100)
    private String providerShipmentId;

    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "pickup_scheduled_at")
    private Instant pickupScheduledAt;

    @Column(name = "pickup_time_slot", length = 20)
    private String pickupTimeSlot;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "estimated_delivery_at")
    private Instant estimatedDeliveryAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "agent_name", length = 100)
    private String agentName;

    @Column(name = "agent_phone_masked", length = 20)
    private String agentPhoneMasked;

    @Column(name = "handoff_notes", columnDefinition = "TEXT")
    private String handoffNotes;

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
