package com.closiq.seller.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class ConfirmProductImageRequest {

    @NotBlank
    String uploadId;

    @NotNull
    Short sortOrder;

    String alt;
}
