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
import com.closiq.identity.web.dto.CompleteRegistrationRequest;
import com.closiq.identity.web.dto.OtpInitiateResponse;
import com.closiq.identity.web.dto.ResetPasswordRequest;
import com.closiq.identity.web.dto.SellerProfileStubResponse;
import com.closiq.identity.web.dto.UserSummaryResponse;
import com.closiq.identity.web.dto.VerifyOtpRequest;
import com.closiq.identity.web.dto.VerifyOtpResponse;
import com.closiq.notification.email.EmailService;
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
    private final EmailService emailService;

    @Transactional
    public OtpInitiateResponse register(String phone, String email) {
        String normalizedPhone = normalizePhone(phone);
        if (userRepository.existsByPhoneAndPhoneVerifiedTrueAndDeletedAtIsNull(normalizedPhone)) {
            throw new ClosiqException(ErrorCode.ALREADY_EXISTS,
                    "An account with this phone number already exists. Please log in instead.");
        }

        String deliveryEmail = email != null && !email.isBlank() ? email.trim().toLowerCase() : null;
        if (deliveryEmail != null) {
            userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(deliveryEmail).ifPresent(existing -> {
                throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "Email is already registered");
            });
        }

        OtpSession session = otpService.createSession(normalizedPhone, OtpPurpose.REGISTER, deliveryEmail);
        return buildOtpInitiateResponse(session, false);
    }

    @Transactional
    public OtpInitiateResponse login(String identifier) {
        AuthIdentifierResolver.ResolvedIdentifier resolved = AuthIdentifierResolver.resolve(identifier);
        User user = userService.requireVerifiedUserForLogin(resolved);

        String deliveryEmail = resolved.type() == AuthIdentifierResolver.Type.EMAIL
                ? resolved.email()
                : user.getEmail();

        OtpSession session = otpService.createSession(user.getPhone(), OtpPurpose.LOGIN, deliveryEmail);
        return buildOtpInitiateResponse(session, true);
    }

    @Transactional
    public AuthSessionResult.TokenPair loginWithPassword(String identifier, String password, String ipAddress, String userAgent) {
        User user = resolveUserForPasswordLogin(identifier);

        if (user.getPasswordHash() == null || !hashUtils.matchesPassword(password, user.getPasswordHash())) {
            throw new ClosiqException(ErrorCode.UNAUTHORIZED, "Invalid phone/email or password");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return issueTokenPair(user, ipAddress, userAgent, false);
    }

    @Transactional
    public OtpInitiateResponse forgotPassword(String identifier) {
        AuthIdentifierResolver.ResolvedIdentifier resolved = AuthIdentifierResolver.resolve(identifier);
        User user = userService.requireVerifiedUserForLogin(resolved);

        String deliveryEmail = resolved.type() == AuthIdentifierResolver.Type.EMAIL
                ? resolved.email()
                : user.getEmail();

        OtpSession session = otpService.createSession(user.getPhone(), OtpPurpose.RESET, deliveryEmail);
        return buildOtpInitiateResponse(session, true);
    }

    @Transactional
    public AuthSessionResult.VerifyResult verifyOtp(VerifyOtpRequest request, String ipAddress, String userAgent) {
        OtpPurpose purpose = mapPurpose(request.getPurpose());
        OtpSession session = loadPendingSession(request.getOtpSessionId());

        rateLimiter.checkVerifyAllowed(session.getPhone());
        validateOtpSession(session, purpose);
        verifyOtpCode(session, request.getOtp());

        session.setStatus(OtpSessionStatus.VERIFIED);
        session.setVerifiedAt(Instant.now());
        otpSessionRepository.save(session);

        Optional<User> existingUser = userRepository.findByPhoneAndDeletedAtIsNull(session.getPhone());

        if (purpose == OtpPurpose.REGISTER) {
            if (existingUser.isPresent()) {
                log.info("Registration OTP verified for existing phone: {}", maskPhone(session.getPhone()));
                return AuthSessionResult.VerifyResult.builder()
                        .response(VerifyOtpResponse.builder()
                                .existingAccount(true)
                                .phone(session.getPhone())
                                .build())
                        .build();
            }

            if (request.getProfile() == null) {
                return AuthSessionResult.VerifyResult.builder()
                        .response(VerifyOtpResponse.builder()
                                .requiresProfile(true)
                                .phone(session.getPhone())
                                .build())
                        .build();
            }

            User user = createRegisteredUser(session, request.getProfile());
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            log.info("User registered: userId={}", user.getId());
            return buildVerifyAuthResult(user, true, ipAddress, userAgent);
        }

        if (purpose == OtpPurpose.RESET) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR,
                    "Use reset-password endpoint after OTP verification for password reset");
        }

        User user = handleLoginVerification(session.getPhone(), existingUser);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        log.info("User authenticated via login OTP: userId={}", user.getId());
        return buildVerifyAuthResult(user, false, ipAddress, userAgent);
    }

    @Transactional
    public AuthSessionResult.TokenPair completeRegistration(
            CompleteRegistrationRequest request,
            String ipAddress,
            String userAgent) {

        OtpSession session = loadVerifiedRegistrationSession(request.getOtpSessionId());
        Optional<User> existingUser = userRepository.findByPhoneAndDeletedAtIsNull(session.getPhone());
        if (existingUser.isPresent()) {
            throw new ClosiqException(ErrorCode.ALREADY_EXISTS,
                    "An account with this phone number already exists. Please log in instead.");
        }

        User user = createRegisteredUser(session, request.getProfile());
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        log.info("User registration completed: userId={}", user.getId());
        return issueTokenPair(user, ipAddress, userAgent, true);
    }

    @Transactional
    public OtpInitiateResponse resendOtp(String otpSessionId) {
        OtpSession session = otpService.resendSession(parseSessionId(otpSessionId));
        return buildOtpInitiateResponse(session, false);
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

        userService.requireLoginEligible(user);
        return issueTokenPair(user, ipAddress, userAgent, false);
    }

    private User resetWithEmailToken(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                        hashUtils.hashToken(request.getResetToken()), Instant.now())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Invalid or expired reset token"));

        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
        User user = token.getUser();
        userService.requireLoginEligible(user);
        return user;
    }

    private User resetWithOtp(ResetPasswordRequest request) {
        OtpSession session = loadPendingSession(request.getOtpSessionId());
        validateOtpSession(session, OtpPurpose.RESET);
        verifyOtpCode(session, request.getOtp());
        session.setStatus(OtpSessionStatus.VERIFIED);
        session.setVerifiedAt(Instant.now());
        otpSessionRepository.save(session);

        User user = userRepository.findByPhoneAndDeletedAtIsNull(session.getPhone())
                .orElseGet(() -> userRepository.findFirstByPhone(session.getPhone())
                        .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "User not found")));
        userService.requireLoginEligible(user);
        return user;
    }

    private User createRegisteredUser(OtpSession session, VerifyOtpRequest.RegistrationProfileRequest profile) {
        if (userService.usernameExists(profile.getUsername())) {
            throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "Username is already taken");
        }

        String profileEmail = profile.getEmail().trim().toLowerCase();
        if (session.getDeliveryEmail() != null
                && !session.getDeliveryEmail().isBlank()
                && !profileEmail.equals(session.getDeliveryEmail())) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR,
                    "Email must match the address used during registration");
        }

        userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(profileEmail)
                .ifPresent(existing -> {
                    throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "Email is already registered");
                });

        User user = userService.createUserWithUsername(
                session.getPhone(),
                profile.getUsername(),
                hashUtils.hashPassword(profile.getPassword()),
                profileEmail,
                profile.getFirstName().trim(),
                profile.getLastName().trim(),
                profile.getGender());

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.sendWelcome(user.getEmail(), profile.getUsername());
            } catch (Exception ex) {
                log.warn("Welcome email failed for {}: {}", user.getEmail(), ex.getMessage());
            }
        }

        return user;
    }

    private OtpSession loadVerifiedRegistrationSession(String sessionId) {
        UUID id = parseSessionId(sessionId);
        OtpSession session = otpSessionRepository.findById(id)
                .orElseThrow(() -> new ClosiqException(ErrorCode.INVALID_OTP, "Invalid OTP session"));

        if (session.getPurpose() != OtpPurpose.REGISTER) {
            throw new ClosiqException(ErrorCode.INVALID_OTP, "OTP session purpose mismatch");
        }

        if (session.getStatus() != OtpSessionStatus.VERIFIED) {
            throw new ClosiqException(ErrorCode.INVALID_OTP, "OTP must be verified before completing registration");
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new ClosiqException(ErrorCode.INVALID_OTP, "Registration session has expired");
        }

        return session;
    }

    private UUID parseSessionId(String sessionId) {
        try {
            return UUID.fromString(sessionId);
        } catch (IllegalArgumentException ex) {
            throw new ClosiqException(ErrorCode.INVALID_OTP, "Invalid OTP session");
        }
    }

    private String normalizePhone(String phone) {
        AuthIdentifierResolver.ResolvedIdentifier resolved = AuthIdentifierResolver.resolve(phone);
        if (resolved.type() != AuthIdentifierResolver.Type.PHONE) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Phone must be a valid Indian mobile number");
        }
        return resolved.phone();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }

    private AuthSessionResult.VerifyResult buildVerifyAuthResult(
            User user, boolean isNewUser, String ipAddress, String userAgent) {
        AuthSessionResult.TokenPair tokens = issueTokenPair(user, ipAddress, userAgent, isNewUser);
        AuthTokenResponse auth = tokens.getAuth();
        return AuthSessionResult.VerifyResult.builder()
                .response(VerifyOtpResponse.builder()
                        .existingAccount(false)
                        .requiresProfile(false)
                        .phone(user.getPhone())
                        .accessToken(auth.getAccessToken())
                        .expiresIn(auth.getExpiresIn())
                        .tokenType(auth.getTokenType())
                        .user(auth.getUser())
                        .isNewUser(auth.getIsNewUser())
                        .build())
                .rawRefreshToken(tokens.getRawRefreshToken())
                .build();
    }

    private User handleLoginVerification(String phone, Optional<User> existingUser) {
        User user = existingUser.orElseGet(() ->
                userRepository.findFirstByPhone(phone).orElse(null));

        if (user == null) {
            throw new ClosiqException(
                    ErrorCode.PHONE_NOT_REGISTERED, ErrorCode.PHONE_NOT_REGISTERED.getDefaultDetail());
        }

        if (!user.isPhoneVerified()) {
            throw new ClosiqException(
                    ErrorCode.PHONE_NOT_REGISTERED, ErrorCode.PHONE_NOT_REGISTERED.getDefaultDetail());
        }

        return userService.requireLoginEligible(user);
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
        int resendAvailableIn = otpService.getResendCooldownRemainingSeconds(session.getId());
        return OtpInitiateResponse.builder()
                .otpSessionId(session.getId().toString())
                .phone(session.getPhone())
                .expiresInSeconds(otpService.getExpirySeconds())
                .resendAvailableInSeconds(resendAvailableIn)
                .isExistingUser(isExistingUser)
                .build();
    }

    private AuthSessionResult.TokenPair issueTokenPair(
            User user, String ipAddress, String userAgent, boolean isNewUser) {
        userService.requireLoginEligible(user);
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
        AuthIdentifierResolver.ResolvedIdentifier resolved = AuthIdentifierResolver.resolve(identifier);
        return userService.requireVerifiedUserForLogin(resolved);
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
