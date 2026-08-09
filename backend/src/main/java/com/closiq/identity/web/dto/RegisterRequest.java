package com.closiq.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Value;

@Value
public class RegisterRequest {

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+91[6-9]\\d{9}$", message = "Phone must be a valid Indian mobile number in E.164 format (+91XXXXXXXXXX)")
    String phone;

    @Email(message = "Email must be valid")
    String email;

    @NotNull(message = "You must accept the terms and conditions")
    Boolean acceptTerms;
}
