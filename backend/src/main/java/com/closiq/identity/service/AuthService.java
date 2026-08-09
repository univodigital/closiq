package com.closiq.identity.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.security.JwtService;
import com.closiq.common.security.RoleType;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.util.HashUtils;
import com.closiq.config.ClosiqProperties;
import com.closiq.identity.domain.OtpPurpose;
import com.closiq.identity.domain.OtpSession;
import com.closiq.identity.domain.OtpSessionStatus;
import com.closiq.identity.domain.PasswordResetToken;
import com.closiq.identity.domain.User;
import com.closiq.identity.mapper.UserMapper;
import com.closiq.identity.repository.OtpSessionRepository;
import com.closiq.identity.repository.PasswordResetTokenRepository;
import com.closiq.identity.repository.UserRepository;
import com.closiq.identity.web.dto.AuthTokenResponse;
import com.closiq.identity.web.dto.OtpInitiateResponse;
import com.closiq.identity.web.dto.ResetPasswordRequest;
import com.closiq.identity.web.dto.SellerProfileStubResponse;
import com.closiq.identity.web.dto.UserSummaryResponse;
import com.closiq.identity.web.dto.VerifyOtpRequest;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpSessionRepository otpSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OtpService otpService;
    private final OtpRateLimiter rateLimiter;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final SellerProfileRepository sellerProfileRepository;
    private final HashUtils hashUtils;
    private final ClosiqProperties properties;

    @Transactional
    public OtpInitiateResponse register(String phone) {
        OtpSession session = otpService.createSession(phone, OtpPurpose.REGISTER);
        return buildOtpInitiateResponse(session, false);
    }

    @Transactional
    public OtpInitiateResponse login(String phone) {
        userRepository.findByPhoneAndDeletedAtIsNull(phone)
                .filter(User::isPhoneVerified)
                .orElseThrow(() -> new ClosiqException(
                        ErrorCode.PHONE_NOT_REGISTERED, ErrorCode.PHONE_NOT_REGISTERED.getDefaultDetail()));

        OtpSession session = otpService.createSession(phone, OtpPurpose.LOGIN);
        return buildOtpInitiateResponse(session, true);
    }

    @Transactional
    public AuthSessionResult.TokenPair loginWithPassword(String identifier, String password, String ipAddress, String userAgent) {
        User user = resolveUserForPasswordLogin(identifier);

        if (user.getPasswordHash() == null || !hashUtils.matchesPassword(password, user.getPasswordHash())) {
            throw new ClosiqException(ErrorCode.UNAUTHORIZED, "Invalid phone/username or password");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return issueTokenPair(user, ipAddress, userAgent, false);
    }

    @Transactional
    public OtpInitiateResponse forgotPassword(String phone) {
        userRepository.findByPhoneAndDeletedAtIsNull(phone)
                .filter(User::isPhoneVerified)
                .orElseThrow(() -> new ClosiqException(
                        ErrorCode.PHONE_NOT_REGISTERED, ErrorCode.PHONE_NOT_REGISTERED.getDefaultDetail()));

        OtpSession session = otpService.createSession(phone, OtpPurpose.RESET);
        return buildOtpInitiateResponse(session, true);
    }

    @Transactional
    public AuthSessionResult.TokenPair verifyOtp(VerifyOtpRequest request, String ipAddress, String userAgent) {
        OtpPurpose purpose = mapPurpose(request.getPurpose());
        OtpSession session = loadPendingSession(request.getOtpSessionId());

        rateLimiter.checkVerifyAllowed(session.getPhone());
        validateOtpSession(session, purpose);
        verifyOtpCode(session, request.getOtp());

        session.setStatus(OtpSessionStatus.VERIFIED);
        session.setVerifiedAt(Instant.now());
        otpSessionRepository.save(session);

        Optional<User> existingUser = userRepository.findByPhoneAndDeletedAtIsNull(session.getPhone());
        boolean isNewUser = purpose == OtpPurpose.REGISTER && existingUser.isEmpty();

        User user = switch (purpose) {
            case REGISTER -> handleRegisterVerification(session, request, existingUser);
            case LOGIN -> handleLoginVerification(existingUser);
            case RESET -> throw new ClosiqException(ErrorCode.VALIDATION_ERROR,
                    "Use reset-password endpoint after OTP verification for password reset");
        };

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User authenticated: userId={}, purpose={}, isNewUser={}", user.getId(), purpose, isNewUser);

        return issueTokenPair(user, ipAddress, userAgent, isNewUser);
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser(UUID userId) {
        User user = userService.requireActiveUser(userId);
        return buildUserSummary(user);
    }

    private UserSummaryResponse buildUserSummary(User user) {
        var profile = userService.requireProfile(user.getId());
        var roles = userService.getUserRoles(user.getId());
        UserSummaryResponse summary = userMapper.toSummaryWithRoleTypes(user, profile, roles);
        return sellerProfileRepository.findByUserId(user.getId())
                .map(sellerProfile -> summary.toBuilder()
                        .sellerProfile(toSellerStub(sellerProfile))
                        .build())
                .orElse(summary);
    }

    private SellerProfileStubResponse toSellerStub(SellerProfile sellerProfile) {
        return SellerProfileStubResponse.builder()
                .sellerId(sellerProfile.getId().toString())
                .businessName(sellerProfile.getBusinessName())
                .verificationStatus(mapSellerVerificationStatus(sellerProfile.getStatus()))
                .build();
    }

    private String mapSellerVerificationStatus(String status) {
        if ("ACTIVE".equals(status)) {
            return "VERIFIED";
        }
        return status;
    }

    @Transactional
    public void logout(UUID userId, String refreshTokenRaw, boolean allDevices) {
        if (allDevices) {
            refreshTokenService.revokeAllForUser(userId);
        } else {
            refreshTokenService.revoke(refreshTokenRaw);
        }
        log.info("User logged out: userId={}, allDevices={}", userId, allDevices);
    }

    @Transactional
    public AuthSessionResult.TokenPair resetPassword(ResetPasswordRequest request, String ipAddress, String userAgent) {
        User user;

        if (request.getResetToken() != null && !request.getResetToken().isBlank()) {
            user = resetWithEmailToken(request);
        } else if (request.getOtpSessionId() != null && request.getOtp() != null) {
            user = resetWithOtp(request);
        } else {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR,
                    "Either resetToken or otpSessionId with otp is required");
        }

        user.setPasswordHash(hashUtils.hashPassword(request.getNewPassword()));
        userRepository.save(user);

        return issueTokenPair(user, ipAddress, userAgent, false);
    }

    private User resetWithEmailToken(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                        hashUtils.hashToken(request.getResetToken()), Instant.now())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Invalid or expired reset token"));

        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
        return token.getUser();
    }

    private User resetWithOtp(ResetPasswordRequest request) {
        OtpSession session = loadPendingSession(request.getOtpSessionId());
        validateOtpSession(session, OtpPurpose.RESET);
        verifyOtpCode(session, request.getOtp());
        session.setStatus(OtpSessionStatus.VERIFIED);
        session.setVerifiedAt(Instant.now());
        otpSessionRepository.save(session);

        return userRepository.findByPhoneAndDeletedAtIsNull(session.getPhone())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "User not found"));
    }

    private User handleRegisterVerification(
            OtpSession session,
            VerifyOtpRequest request,
            Optional<User> existingUser) {

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        if (request.getProfile() == null) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR,
                    "Profile is required for new user registration");
        }

        var profile = request.getProfile();
        if (userService.usernameExists(profile.getUsername())) {
            throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "Username is already taken");
        }

        return userService.createUserWithUsername(
                session.getPhone(),
                profile.getUsername(),
                hashUtils.hashPassword(profile.getPassword()),
                profile.getEmail());
    }

    private User handleLoginVerification(Optional<User> existingUser) {
        return existingUser
                .filter(User::isPhoneVerified)
                .orElseThrow(() -> new ClosiqException(
                        ErrorCode.PHONE_NOT_REGISTERED, ErrorCode.PHONE_NOT_REGISTERED.getDefaultDetail()));
    }

    private OtpSession loadPendingSession(String sessionId) {
        UUID id;
        try {
            id = UUID.fromString(sessionId);
        } catch (IllegalArgumentException ex) {
            throw new ClosiqException(ErrorCode.INVALID_OTP, "Invalid OTP session");
        }

        return otpSessionRepository.findById(id)
                .orElseThrow(() -> new ClosiqException(ErrorCode.INVALID_OTP, "Invalid OTP session"));
    }

    private void validateOtpSession(OtpSession session, OtpPurpose expectedPurpose) {
        if (session.getPurpose() != expectedPurpose) {
            throw new ClosiqException(ErrorCode.INVALID_OTP, "OTP session purpose mismatch");
        }

        if (session.getStatus() == OtpSessionStatus.LOCKED) {
            if (session.getLockedUntil() != null && session.getLockedUntil().isAfter(Instant.now())) {
                throw new ClosiqException(ErrorCode.INVALID_OTP, "OTP session is locked. Try again later.");
            }
            session.setStatus(OtpSessionStatus.PENDING);
            session.setAttempts((short) 0);
            session.setLockedUntil(null);
        }

        if (session.getStatus() != OtpSessionStatus.PENDING) {
            throw new ClosiqException(ErrorCode.INVALID_OTP, "OTP session is no longer valid");
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setStatus(OtpSessionStatus.EXPIRED);
            otpSessionRepository.save(session);
            throw new ClosiqException(ErrorCode.INVALID_OTP, "OTP has expired");
        }
    }

    private void verifyOtpCode(OtpSession session, String otp) {
        if (!hashUtils.hashOtp(otp).equals(session.getOtpHash())) {
            session.setAttempts((short) (session.getAttempts() + 1));

            if (session.getAttempts() >= properties.getOtp().getMaxVerifyAttempts()) {
                session.setStatus(OtpSessionStatus.LOCKED);
                session.setLockedUntil(Instant.now().plusSeconds(
                        properties.getOtp().getLockoutMinutes() * 60L));
            }

            otpSessionRepository.save(session);
            throw new ClosiqException(ErrorCode.INVALID_OTP, "Invalid OTP");
        }
    }

    private OtpInitiateResponse buildOtpInitiateResponse(OtpSession session, boolean isExistingUser) {
        return OtpInitiateResponse.builder()
                .otpSessionId(session.getId().toString())
                .phone(session.getPhone())
                .expiresInSeconds(otpService.getExpirySeconds())
                .resendAvailableInSeconds(otpService.getResendCooldownSeconds())
                .isExistingUser(isExistingUser)
                .build();
    }

    private AuthSessionResult.TokenPair issueTokenPair(
            User user, String ipAddress, String userAgent, boolean isNewUser) {
        UserPrincipal principal = userService.buildPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);
        RefreshTokenService.IssuedRefreshToken refreshToken =
                refreshTokenService.issue(user, ipAddress, userAgent);

        AuthTokenResponse response = AuthTokenResponse.builder()
                .accessToken(accessToken)
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .tokenType("Bearer")
                .user(buildUserSummary(user))
                .isNewUser(isNewUser)
                .build();

        return AuthSessionResult.TokenPair.builder()
                .auth(response)
                .rawRefreshToken(refreshToken.rawToken())
                .build();
    }

    private User resolveUserForPasswordLogin(String identifier) {
        String normalized = identifier;
        if (identifier.matches("^[6-9]\\d{9}$")) {
            normalized = "+91" + identifier;
        }

        if (normalized.matches("^\\+91[6-9]\\d{9}$")) {
            return userRepository.findByPhoneAndDeletedAtIsNull(normalized)
                    .filter(User::isPhoneVerified)
                    .filter(user -> user.getStatus() == com.closiq.identity.domain.UserStatus.ACTIVE)
                    .orElseThrow(() -> new ClosiqException(
                            ErrorCode.UNAUTHORIZED, "Invalid phone/username or password"));
        }
        return userService.findByUsername(normalized);
    }

    private OtpPurpose mapPurpose(String purpose) {
        return switch (purpose) {
            case "REGISTER" -> OtpPurpose.REGISTER;
            case "LOGIN" -> OtpPurpose.LOGIN;
            case "RESET_PASSWORD" -> OtpPurpose.RESET;
            default -> throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Invalid OTP purpose");
        };
    }
}
