package com.closiq.seller.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.util.List;

@Value
public class CreateSellerProductRequest {

    @NotBlank
    @Size(min = 5, max = 100)
    String title;

    @NotBlank
    @Size(min = 50, max = 2000)
    String description;

    @NotNull
    java.util.UUID categoryId;

    @NotBlank
    String occasion;

    @Size(max = 100)
    String designer;

    @NotNull
    @Min(100)
    @Max(50000)
    Long pricePerDay;

    @NotNull
    @Min(100)
    @Max(100000)
    Long deposit;

    @NotEmpty
    @Valid
    List<VariantInput> variants;

    @NotBlank
    @Size(max = 50)
    String city;

    @Value
    public static class VariantInput {
        @NotBlank
        String size;

        @NotNull
        @Min(1)
        Integer quantity;
    }
}
