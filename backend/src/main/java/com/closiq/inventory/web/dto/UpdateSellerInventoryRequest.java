package com.closiq.inventory.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.util.List;

@Value
public class UpdateSellerInventoryRequest {

    @NotEmpty
    @Valid
    List<VariantQuantity> variants;

    @Value
    public static class VariantQuantity {
        @NotNull
        String variantId;

        @NotNull
        Integer quantity;
    }
}
