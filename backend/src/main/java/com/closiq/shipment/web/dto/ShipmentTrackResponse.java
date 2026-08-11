package com.closiq.shipment.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class ShipmentTrackResponse {

    UUID shipmentId;
    UUID bookingId;
    String type;
    String provider;
    String trackingNumber;
    String status;
    Instant estimatedDeliveryAt;
    Instant pickupScheduledAt;
    String pickupTimeSlot;
    List<ShipmentEventResponse> events;
    String agentName;
    String agentPhone;
}
