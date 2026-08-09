package com.closiq.identity.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AuthTokenResponse {

    String accessToken;
    long expiresIn;
    String tokenType;
    UserSummaryResponse user;
    Boolean isNewUser;
}
