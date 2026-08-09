package com.closiq.seller.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.time.Instant;

@Value
public class AcceptSellerBookingRequest {

    @NotNull
    Instant estimatedPrepBy;

    String notes;
}
