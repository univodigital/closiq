package com.closiq.seller.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class UpdateSellerProductRequest {

    @Size(min = 5, max = 100)
    String title;

    @Size(min = 50, max = 2000)
    String description;

    java.util.UUID categoryId;

    @Size(max = 100)
    String designer;

    @Min(100)
    @Max(50000)
    Long pricePerDay;

    @Min(100)
    @Max(100000)
    Long deposit;

    @Size(max = 50)
    String city;
}
