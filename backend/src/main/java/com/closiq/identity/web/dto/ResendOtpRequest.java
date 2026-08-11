package com.closiq.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class ResendOtpRequest {

    @NotBlank(message = "OTP session ID is required")
    String otpSessionId;
}
