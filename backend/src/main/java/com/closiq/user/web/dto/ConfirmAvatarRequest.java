package com.closiq.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class ConfirmAvatarRequest {

    @NotBlank
    String uploadId;
}
