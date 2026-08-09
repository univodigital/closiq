package com.closiq.seller.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class ProductImageUploadUrlRequest {

    @NotBlank
    String contentType;

    String fileName;
}
