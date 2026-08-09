package com.closiq.shipment.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ShipmentResponse {

    UUID shipmentId;
    UUID bookingId;
    String type;
    String provider;
    String trackingNumber;
    String status;
    Instant pickupScheduledAt;
    Instant estimatedDeliveryAt;
}
