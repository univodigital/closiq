package com.closiq.cart.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.time.LocalDate;

@Value
public class CartItemRequest {

    @NotBlank
    String productSlug;

    @NotBlank
    String variantSize;

    @NotNull
    LocalDate rentalStartDate;

    @NotNull
    LocalDate rentalEndDate;
}
