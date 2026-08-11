package com.closiq.identity.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VerifyOtpResponse {

    Boolean existingAccount;
    Boolean requiresProfile;
    String phone;
    String accessToken;
    Long expiresIn;
    String tokenType;
    UserSummaryResponse user;
    Boolean isNewUser;
}
