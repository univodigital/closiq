package com.closiq.inventory.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.time.LocalDate;

@Value
public class CreateInventoryBlockRequest {

    @NotNull
    String productId;

    @NotNull
    String variantId;

    @NotNull
    LocalDate startDate;

    @NotNull
    LocalDate endDate;

    String reason;
}
