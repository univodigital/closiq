package com.closiq.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Value;

@Value
public class VerifyPhoneChangeOtpRequest {

    @NotBlank
    String otpSessionId;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$")
    String otp;
}
