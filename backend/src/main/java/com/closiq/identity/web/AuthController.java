package com.closiq.identity.web;

import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.config.ClosiqProperties;
import com.closiq.identity.service.AuthService;
import com.closiq.identity.service.AuthSessionResult;
import com.closiq.identity.service.RefreshTokenService;
import com.closiq.identity.web.dto.AuthTokenResponse;
import com.closiq.identity.web.dto.ForgotPasswordRequest;
import com.closiq.identity.web.dto.LoginRequest;
import com.closiq.identity.web.dto.OtpInitiateResponse;
import com.closiq.identity.web.dto.PasswordLoginRequest;
import com.closiq.identity.web.dto.RefreshTokenResponse;
import com.closiq.identity.web.dto.RegisterRequest;
import com.closiq.identity.web.dto.ResetPasswordRequest;
import com.closiq.identity.web.dto.UserSummaryResponse;
import com.closiq.identity.web.dto.VerifyOtpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Phone OTP authentication, JWT sessions, password reset")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final ClosiqProperties properties;

    @PostMapping("/register")
    @Operation(summary = "Start registration — send OTP to phone")
    public ResponseEntity<ApiResponse<OtpInitiateResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        if (!Boolean.TRUE.equals(request.getAcceptTerms())) {
            throw new com.closiq.common.exception.ClosiqException(
                    com.closiq.common.exception.ErrorCode.VALIDATION_ERROR,
                    "You must accept the terms and conditions");
        }

        OtpInitiateResponse response = authService.register(request.getPhone());
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(httpRequest)));
    }

    @PostMapping("/login")
    @Operation(summary = "Send OTP to registered phone for login")
    public ResponseEntity<ApiResponse<OtpInitiateResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        OtpInitiateResponse response = authService.login(request.getPhone());
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(httpRequest)));
    }

    @PostMapping("/login-password")
    @Operation(summary = "Login with phone/username and password")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> loginWithPassword(
            @Valid @RequestBody PasswordLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        AuthSessionResult.TokenPair result = authService.loginWithPassword(
                request.getIdentifier(),
                request.getPassword(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));

        writeRefreshCookie(httpResponse, result.getRawRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(result.getAuth(), ClosiqRequestIdFilter.getRequestId(httpRequest)));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP — complete registration or login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        AuthSessionResult.TokenPair result = authService.verifyOtp(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));

        writeRefreshCookie(httpResponse, result.getRawRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(result.getAuth(), ClosiqRequestIdFilter.getRequestId(httpRequest)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate refresh token and end session")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) Map<String, Boolean> body,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        boolean allDevices = body != null && Boolean.TRUE.equals(body.get("allDevices"));
        if (allDevices && principal == null) {
            throw new com.closiq.common.exception.ClosiqException(
                    com.closiq.common.exception.ErrorCode.UNAUTHORIZED,
                    "Authentication required to logout all devices");
        }

        UUID userId = principal != null ? principal.userId() : null;
        authService.logout(userId, extractRefreshToken(httpRequest), allDevices);
        clearRefreshCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Issue new access token using refresh cookie")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String refreshToken = extractRefreshToken(httpRequest);
        if (refreshToken == null) {
            throw new com.closiq.common.exception.ClosiqException(
                    com.closiq.common.exception.ErrorCode.UNAUTHORIZED, "Refresh token missing");
        }

        AuthSessionResult.RefreshPair result = refreshTokenService.refreshAccessToken(
                refreshToken,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));

        writeRefreshCookie(httpResponse, result.getRawRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(result.getAuth(), ClosiqRequestIdFilter.getRequestId(httpRequest)));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send OTP to reset password via phone")
    public ResponseEntity<ApiResponse<OtpInitiateResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        OtpInitiateResponse response = authService.forgotPassword(request.getPhone());
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(httpRequest)));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set new password using reset token or OTP session")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        AuthSessionResult.TokenPair result = authService.resetPassword(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));

        writeRefreshCookie(httpResponse, result.getRawRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(result.getAuth(), ClosiqRequestIdFilter.getRequestId(httpRequest)));
    }

    @GetMapping("/me")
    @Operation(summary = "Return authenticated user profile summary")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> me(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        UserSummaryResponse user = authService.getCurrentUser(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(user, ClosiqRequestIdFilter.getRequestId(httpRequest)));
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (RefreshTokenService.REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeRefreshCookie(HttpServletResponse response, String rawToken) {
        ResponseCookie cookie = ResponseCookie.from(RefreshTokenService.REFRESH_COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(properties.getAuth().isRefreshCookieSecure())
                .path(RefreshTokenService.REFRESH_COOKIE_PATH)
                .maxAge(refreshTokenService.getRefreshCookieMaxAgeSeconds())
                .sameSite("Strict")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(RefreshTokenService.REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(properties.getAuth().isRefreshCookieSecure())
                .path(RefreshTokenService.REFRESH_COOKIE_PATH)
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
