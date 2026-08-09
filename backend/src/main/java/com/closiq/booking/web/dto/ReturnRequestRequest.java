package com.closiq.booking.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
public class ReturnRequestRequest {

    @NotNull
    LocalDate pickupDate;

    @NotNull
    String pickupTimeSlot;

    UUID addressId;
}
