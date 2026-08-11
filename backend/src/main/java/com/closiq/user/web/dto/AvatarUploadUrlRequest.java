package com.closiq.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Value;

@Value
public class AvatarUploadUrlRequest {

    @NotBlank
    String fileName;

    @NotBlank
    String contentType;

    @NotNull
    @Positive
    Long fileSize;
}
