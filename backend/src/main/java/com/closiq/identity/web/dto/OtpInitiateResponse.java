package com.closiq.identity.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OtpInitiateResponse {

    String otpSessionId;
    String phone;
    int expiresInSeconds;
    int resendAvailableInSeconds;
    boolean isExistingUser;
}
