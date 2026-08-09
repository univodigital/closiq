package com.closiq.payment.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
public class CheckoutCalculateRequest {

    @NotNull
    UUID productId;

    @NotNull
    UUID variantId;

    @NotNull
    LocalDate rentalStartDate;

    @NotNull
    LocalDate rentalEndDate;

    String pincode;

    String couponCode;
}
