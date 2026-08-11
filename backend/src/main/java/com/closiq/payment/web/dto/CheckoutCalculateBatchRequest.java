package com.closiq.payment.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Value
public class CheckoutCalculateBatchRequest {

    @NotNull
    @Valid
    List<LineItem> items;

    String pincode;

    String couponCode;

    @Value
    public static class LineItem {

        @NotNull
        UUID productId;

        @NotNull
        UUID variantId;

        @NotNull
        LocalDate rentalStartDate;

        @NotNull
        LocalDate rentalEndDate;
    }
}
