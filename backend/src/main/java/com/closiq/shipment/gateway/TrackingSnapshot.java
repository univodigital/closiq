package com.closiq.shipment.gateway;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class TrackingSnapshot {

    String status;
    String agentName;
    String agentPhoneMasked;
    Instant estimatedDeliveryAt;
}
