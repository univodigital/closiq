package com.closiq.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class AddWishlistRequest {

    @NotBlank(message = "Product ID is required")
    String productId;
}
