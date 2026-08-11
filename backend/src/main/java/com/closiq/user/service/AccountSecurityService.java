package com.closiq.user.service;

import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.util.HashUtils;
import com.closiq.config.ClosiqProperties;
import com.closiq.identity.domain.OtpPurpose;
import com.closiq.identity.domain.OtpSession;
import com.closiq.identity.domain.OtpSessionStatus;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.repository.OtpSessionRepository;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.identity.repository.UserRepository;
import com.closiq.identity.service.AuthIdentifierResolver;
import com.closiq.identity.service.OtpRateLimiter;
import com.closiq.identity.service.OtpService;
import com.closiq.identity.service.RefreshTokenService;
import com.closiq.identity.service.UserService;
import com.closiq.identity.web.dto.OtpInitiateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSecurityService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final OtpSessionRepository otpSessionRepository;
    private final OtpService otpService;
    private final OtpRateLimiter rateLimiter;
    private final UserService userService;
    private final AccountChangeStateService changeStateService;
    private final RefreshTokenService refreshTokenService;
    private final HashUtils hashUtils;
    private final ClosiqProperties properties;

    @Transactional
    public OtpInitiateResponse initiatePhoneChange(UUID userId) {
        User user = userService.requireActiveUser(userId);
        changeStateService.clearPhoneChangeState(userId);
        OtpSession session = otpService.createSession(user.getPhone(), OtpPurpose.CHANGE_PHONE_OLD, user.getEmail());
        return buildOtpResponse(session);
    }

    @Transactional
    public void verifyOldPhone(UUID userId, String otpSessionId, String otp) {
        User user = userService.requireActiveUser(userId);
        OtpSession session = verifySession(otpSessionId, OtpPurpose.CHANGE_PHONE_OLD, user.getPhone(), otp);
        session.setStatus(OtpSessionStatus.VERIFIED);
        session.setVerifiedAt(Instant.now());
        otpSessionRepository.save(session);
        changeStateService.markOldPhoneVerified(userId);
        log.info("Old phone verified for phone change: userId={}", userId);
    }

    @Transactional
    public OtpInitiateResponse sendNewPhoneOtp(UUID userId, String newPhone) {
        User user = userService.requireActiveUser(userId);
        AccountChangeStateService.PhoneChangeState state = changeStateService.getPhoneChangeState(userId)
                .filter(AccountChangeStateService.PhoneChangeState::oldPhoneVerified)
                .orElseThrow(() -> new ClosiqException(
                        ErrorCode.VALIDATION_ERROR, "Verify your current phone number before entering a new one"));

        String normalized = normalizePhone(newPhone);
        if (normalized.equals(user.getPhone())) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "New phone must differ from current phone");
        }

        ensurePhoneAvailable(normalized, userId);
        changeStateService.setPendingNewPhone(userId, normalized);
        OtpSession session = otpService.createSession(normalized, OtpPurpose.CHANGE_PHONE_NEW, null);
        return buildOtpResponse(session);
    }

    @Transactional
    public void completePhoneChange(UUID userId, String otpSessionId, String otp) {
        User user = userService.requireActiveUser(userId);
        AccountChangeStateService.PhoneChangeState state = changeStateService.getPhoneChangeState(userId)
                .filter(AccountChangeStateService.PhoneChangeState::oldPhoneVerified)
                .filter(s -> s.newPhone() != null && !s.newPhone().isBlank())
                .orElseThrow(() -> new ClosiqException(
                        ErrorCode.VALIDATION_ERROR, "Complete old and new phone verification before updating"));

        String newPhone = state.newPhone();
        verifySession(otpSessionId, OtpPurpose.CHANGE_PHONE_NEW, newPhone, otp);
        ensurePhoneAvailable(newPhone, userId);

        user.setPhone(newPhone);
        user.setPhoneVerified(true);
        userRepository.save(user);
        changeStateService.clearPhoneChangeState(userId);
        refreshTokenService.revokeAllForUser(userId);
        log.info("Phone number changed: userId={}", userId);
    }

    @Transactional
    public OtpInitiateResponse requestEmailChange(UUID userId, String newEmail) {
        User user = userService.requireActiveUser(userId);
        String normalized = newEmail.trim().toLowerCase();
        if (normalized.equalsIgnoreCase(user.getEmail())) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "New email must differ from current email");
        }

        userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalized)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ClosiqException(
                            ErrorCode.ALREADY_EXISTS, "That email is already associated with another account");
                });

        user.setPendingEmail(normalized);
        user.setEmailVerified(false);
        userRepository.save(user);

        OtpSession session = otpService.createSession(user.getPhone(), OtpPurpose.CHANGE_EMAIL, normalized);
        changeStateService.storeEmailChangeSession(session.getId(), userId, normalized);
        return buildOtpResponse(session);
    }

    @Transactional
    public void verifyEmailChange(UUID userId, String otpSessionId, String otp) {
        User user = userService.requireActiveUser(userId);
        AccountChangeStateService.EmailChangeState state = changeStateService
                .getEmailChangeSession(parseSessionId(otpSessionId))
                .orElseThrow(() -> new ClosiqException(ErrorCode.INVALID_OTP, "Email change session expired"));

        String pendingEmail = state.pendingEmail();
        if (user.getPendingEmail() == null || !pendingEmail.equalsIgnoreCase(user.getPendingEmail())) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "No pending email change for this account");
        }

        verifySession(otpSessionId, OtpPurpose.CHANGE_EMAIL, user.getPhone(), otp);

        userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(pendingEmail)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ClosiqException(
                            ErrorCode.ALREADY_EXISTS, "That email is already associated with another account");
                });

        user.setEmail(pendingEmail);
        user.setPendingEmail(null);
        user.setEmailVerified(true);
        userRepository.save(user);
        changeStateService.clearEmailChangeState(userId, parseSessionId(otpSessionId));
        log.info("Email changed: userId={}", userId);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword, String refreshTokenRaw) {
        User user = userService.requireActiveUser(userId);
        if (user.getPasswordHash() == null
                || !hashUtils.matchesPassword(currentPassword, user.getPasswordHash())) {
            throw new ClosiqException(ErrorCode.UNAUTHORIZED, "Current password is incorrect");
        }
        if (hashUtils.matchesPassword(newPassword, user.getPasswordHash())) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "New password must differ from current password");
        }

        user.setPasswordHash(hashUtils.hashPassword(newPassword));
        userRepository.save(user);
        refreshTokenService.revokeAllOtherSessions(userId, refreshTokenRaw);
        log.info("Password changed: userId={}", userId);
    }

    @Transactional
    public void changeUsername(UUID userId, String username) {
        User user = userService.requireActiveUser(userId);
        UserProfile profile = userService.requireProfile(userId);

        if (profile.getUsernameChangedAt() != null) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Username can only be changed once");
        }

        String normalized = username.trim();
        if (normalized.equalsIgnoreCase(profile.getUsername())) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Choose a different username");
        }
        if (userService.usernameExists(normalized)) {
            throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "Username is already taken");
        }

        profile.setUsername(normalized);
        profile.setUsernameChangedAt(Instant.now());
        userProfileRepository.save(profile);
        log.info("Username changed: userId={}", userId);
    }

    private void ensurePhoneAvailable(String phone, UUID userId) {
        userRepository.findByPhoneAndDeletedAtIsNull(phone)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ClosiqException(
                            ErrorCode.ALREADY_EXISTS,
                            "That phone number is already associated with another account");
                });
    }

    private OtpSession verifySession(String otpSessionId, OtpPurpose purpose, String expectedPhone, String otp) {
        OtpSession session = loadPendingSession(otpSessionId);
        rateLimiter.checkVerifyAllowed(session.getPhone());
        validateOtpSession(session, purpose, expectedPhone);
        verifyOtpCode(session, otp);
        return session;
    }

    private OtpSession loadPendingSession(String sessionId) {
        UUID id = parseSessionId(sessionId);
        return otpSessionRepository.findById(id)
                .orElseThrow(() -> new ClosiqException(ErrorCode.INVALID_OTP, "Invalid OTP session"));
    }

    private UUID parseSessionId(String sessionId) {
        try {
            return UUID.fromString(sessionId);
        } catch (IllegalArgumentException ex) {
            throw new ClosiqException(ErrorCode.INVALID_OTP, "Invalid OTP session");
        }
    }

    private void validateOtpSession(OtpSession session, OtpPurpose expectedPurpose, String expectedPhone) {
        if (session.getPurpose() != expectedPurpose) {
            throw new ClosiqException(ErrorCode.INVALID_OTP, "OTP session purpose mismatch");
        }
        if (!session.getPhone().equals(expectedPhone)) {
            throw new ClosiqException(ErrorCode.INVALID_OTP, "OTP session phone mismatch");
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

    private String normalizePhone(String phone) {
        AuthIdentifierResolver.ResolvedIdentifier resolved = AuthIdentifierResolver.resolve(phone);
        if (resolved.type() != AuthIdentifierResolver.Type.PHONE) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Enter a valid phone number");
        }
        return resolved.phone();
    }

    private OtpInitiateResponse buildOtpResponse(OtpSession session) {
        return OtpInitiateResponse.builder()
                .otpSessionId(session.getId().toString())
                .phone(session.getPhone())
                .expiresInSeconds(otpService.getExpirySeconds())
                .resendAvailableInSeconds(otpService.getResendCooldownRemainingSeconds(session.getId()))
                .isExistingUser(true)
                .build();
    }
}
