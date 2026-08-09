package com.closiq.shipment.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ReadyForPickupResponse {

    String status;
    UUID shipmentId;
    Instant pickupScheduledAt;
}
