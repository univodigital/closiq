package com.closiq.identity.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RefreshTokenResponse {

    String accessToken;
    long expiresIn;
    String tokenType;
}
