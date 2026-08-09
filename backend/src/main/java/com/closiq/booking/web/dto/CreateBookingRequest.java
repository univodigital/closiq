package com.closiq.booking.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
public class CreateBookingRequest {

    @NotNull
    UUID productId;

    @NotNull
    UUID variantId;

    @NotNull
    LocalDate rentalStartDate;

    @NotNull
    LocalDate rentalEndDate;

    UUID deliveryAddressId;

    @Size(max = 200)
    String customerNotes;
}
