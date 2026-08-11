package com.closiq.identity.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class CompleteRegistrationRequest {

    @NotBlank(message = "OTP session ID is required")
    String otpSessionId;

    @NotNull(message = "Profile is required")
    @Valid
    VerifyOtpRequest.RegistrationProfileRequest profile;
}
