package com.closiq.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class LoginRequest {

    /** Indian mobile number or email address. */
    @NotBlank(message = "Phone or email is required")
    @Size(max = 255, message = "Identifier is too long")
    String identifier;
}
