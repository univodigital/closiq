package com.closiq.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class PasswordLoginRequest {

    /** Phone in E.164 format (+91XXXXXXXXXX) or username. */
    @NotBlank(message = "Phone or username is required")
    @Size(max = 255, message = "Identifier is too long")
    String identifier;

    @NotBlank(message = "Password is required")
    String password;
}
