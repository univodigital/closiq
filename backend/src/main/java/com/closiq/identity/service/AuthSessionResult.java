package com.closiq.identity.service;

import com.closiq.identity.web.dto.AuthTokenResponse;
import com.closiq.identity.web.dto.RefreshTokenResponse;
import com.closiq.identity.web.dto.VerifyOtpResponse;
import lombok.Builder;
import lombok.Value;

public final class AuthSessionResult {

    private AuthSessionResult() {
    }

    @Value
    @Builder
    public static class VerifyResult {
        VerifyOtpResponse response;
        String rawRefreshToken;
    }

    @Value
    @Builder
    public static class TokenPair {
        AuthTokenResponse auth;
        String rawRefreshToken;
    }

    @Value
    @Builder
    public static class RefreshPair {
        RefreshTokenResponse auth;
        String rawRefreshToken;
    }
}
