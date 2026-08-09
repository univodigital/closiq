package com.closiq.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Value;

@Value
public class LoginRequest {

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+91[6-9]\\d{9}$", message = "Phone must be a valid Indian mobile number in E.164 format (+91XXXXXXXXXX)")
    String phone;
}
