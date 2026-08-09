package com.closiq.shipment.gateway;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class CreateShipmentResult {

    String providerShipmentId;
    String trackingNumber;
    Instant pickupScheduledAt;
    Instant estimatedDeliveryAt;
}
