package com.closiq.shipment.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ShipmentStatusResponse {

    String status;
    Instant updatedAt;
}
