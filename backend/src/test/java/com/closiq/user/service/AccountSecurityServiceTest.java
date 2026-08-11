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
import com.closiq.identity.service.OtpRateLimiter;
import com.closiq.identity.service.OtpService;
import com.closiq.identity.service.RefreshTokenService;
import com.closiq.identity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSecurityServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private OtpSessionRepository otpSessionRepository;
    @Mock private OtpService otpService;
    @Mock private OtpRateLimiter rateLimiter;
    @Mock private UserService userService;
    @Mock private AccountChangeStateService changeStateService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private HashUtils hashUtils;
    @Mock private ClosiqProperties properties;

    @InjectMocks
    private AccountSecurityService accountSecurityService;

    private UUID userId;
    private User user;
    private UUID oldSessionId;
    private UUID newSessionId;
    private OtpSession oldSession;
    private OtpSession newSession;
    private ClosiqProperties.Otp otpProps;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        oldSessionId = UUID.randomUUID();
        newSessionId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .phone("+919876543210")
                .email("user@example.com")
                .emailVerified(true)
                .phoneVerified(true)
                .passwordHash("hashed-current")
                .build();

        oldSession = pendingSession(oldSessionId, "+919876543210", OtpPurpose.CHANGE_PHONE_OLD);
        newSession = pendingSession(newSessionId, "+919111111111", OtpPurpose.CHANGE_PHONE_NEW);

        otpProps = new ClosiqProperties.Otp();
        otpProps.setMaxVerifyAttempts(5);
        otpProps.setLockoutMinutes(15);
    }

    @Test
    void sendNewPhoneOtp_requiresOldPhoneVerified() {
        when(userService.requireActiveUser(userId)).thenReturn(user);
        when(changeStateService.getPhoneChangeState(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountSecurityService.sendNewPhoneOtp(userId, "+919111111111"))
                .isInstanceOf(ClosiqException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void sendNewPhoneOtp_rejectsDuplicatePhone() {
        when(userService.requireActiveUser(userId)).thenReturn(user);
        when(changeStateService.getPhoneChangeState(userId))
                .thenReturn(Optional.of(new AccountChangeStateService.PhoneChangeState(true, null)));
        when(userRepository.findByPhoneAndDeletedAtIsNull("+919111111111"))
                .thenReturn(Optional.of(User.builder().id(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> accountSecurityService.sendNewPhoneOtp(userId, "+919111111111"))
                .isInstanceOf(ClosiqException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_EXISTS);
    }

    @Test
    void completePhoneChange_requiresBothVerifications() {
        when(userService.requireActiveUser(userId)).thenReturn(user);
        when(changeStateService.getPhoneChangeState(userId))
                .thenReturn(Optional.of(new AccountChangeStateService.PhoneChangeState(true, null)));

        assertThatThrownBy(() -> accountSecurityService.completePhoneChange(userId, newSessionId.toString(), "123456"))
                .isInstanceOf(ClosiqException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);

        verify(userRepository, never()).save(any());
    }

    @Test
    void completePhoneChange_updatesPhoneAndRevokesSessions() {
        String newPhone = "+919111111111";
        when(userService.requireActiveUser(userId)).thenReturn(user);
        when(changeStateService.getPhoneChangeState(userId))
                .thenReturn(Optional.of(new AccountChangeStateService.PhoneChangeState(true, newPhone)));
        when(otpSessionRepository.findById(newSessionId)).thenReturn(Optional.of(newSession));
        when(hashUtils.hashOtp("123456")).thenReturn("hashed-otp");
        when(userRepository.findByPhoneAndDeletedAtIsNull(newPhone)).thenReturn(Optional.empty());

        accountSecurityService.completePhoneChange(userId, newSessionId.toString(), "123456");

        assertThat(user.getPhone()).isEqualTo(newPhone);
        verify(userRepository).save(user);
        verify(changeStateService).clearPhoneChangeState(userId);
        verify(refreshTokenService).revokeAllForUser(userId);
    }

    @Test
    void requestEmailChange_setsPendingEmailWithoutReplacingCurrent() {
        when(userService.requireActiveUser(userId)).thenReturn(user);
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("new@example.com"))
                .thenReturn(Optional.empty());
        when(otpService.createSession(user.getPhone(), OtpPurpose.CHANGE_EMAIL, "new@example.com"))
                .thenReturn(oldSession);
        when(otpService.getExpirySeconds()).thenReturn(300);
        when(otpService.getResendCooldownRemainingSeconds(oldSessionId)).thenReturn(60);

        accountSecurityService.requestEmailChange(userId, "new@example.com");

        assertThat(user.getPendingEmail()).isEqualTo("new@example.com");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.isEmailVerified()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmailChange_promotesPendingEmailAfterOtp() {
        UUID emailSessionId = oldSessionId;
        user.setPendingEmail("new@example.com");
        user.setEmailVerified(false);

        when(userService.requireActiveUser(userId)).thenReturn(user);
        when(changeStateService.getEmailChangeSession(emailSessionId))
                .thenReturn(Optional.of(new AccountChangeStateService.EmailChangeState("new@example.com")));
        when(otpSessionRepository.findById(emailSessionId)).thenReturn(Optional.of(
                pendingSession(emailSessionId, user.getPhone(), OtpPurpose.CHANGE_EMAIL)));
        when(hashUtils.hashOtp("123456")).thenReturn("hashed-otp");
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("new@example.com"))
                .thenReturn(Optional.empty());

        accountSecurityService.verifyEmailChange(userId, emailSessionId.toString(), "123456");

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPendingEmail()).isNull();
        assertThat(user.isEmailVerified()).isTrue();
        verify(changeStateService).clearEmailChangeState(userId, emailSessionId);
    }

    @Test
    void changePassword_rejectsWrongCurrentPassword() {
        when(userService.requireActiveUser(userId)).thenReturn(user);
        when(hashUtils.matchesPassword("wrong", "hashed-current")).thenReturn(false);

        assertThatThrownBy(() -> accountSecurityService.changePassword(userId, "wrong", "newPass123!", "refresh"))
                .isInstanceOf(ClosiqException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    @Test
    void changePassword_revokesOtherSessions() {
        when(userService.requireActiveUser(userId)).thenReturn(user);
        when(hashUtils.matchesPassword("current", "hashed-current")).thenReturn(true);
        when(hashUtils.matchesPassword("newPass123!", "hashed-current")).thenReturn(false);
        when(hashUtils.hashPassword("newPass123!")).thenReturn("hashed-new");

        accountSecurityService.changePassword(userId, "current", "newPass123!", "refresh-token");

        verify(refreshTokenService).revokeAllOtherSessions(userId, "refresh-token");
        verify(userRepository).save(user);
    }

    @Test
    void changeUsername_allowsFirstChangeOnly() {
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .username("oldname")
                .usernameChangedAt(null)
                .build();

        when(userService.requireActiveUser(userId)).thenReturn(user);
        when(userService.requireProfile(userId)).thenReturn(profile);
        when(userService.usernameExists("newname")).thenReturn(false);

        accountSecurityService.changeUsername(userId, "newname");

        assertThat(profile.getUsername()).isEqualTo("newname");
        assertThat(profile.getUsernameChangedAt()).isNotNull();
        verify(userProfileRepository).save(profile);
    }

    @Test
    void changeUsername_rejectsSecondChange() {
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .username("oldname")
                .usernameChangedAt(Instant.now())
                .build();

        when(userService.requireActiveUser(userId)).thenReturn(user);
        when(userService.requireProfile(userId)).thenReturn(profile);

        assertThatThrownBy(() -> accountSecurityService.changeUsername(userId, "newname"))
                .isInstanceOf(ClosiqException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
    }

    private OtpSession pendingSession(UUID id, String phone, OtpPurpose purpose) {
        return OtpSession.builder()
                .id(id)
                .phone(phone)
                .otpHash("hashed-otp")
                .purpose(purpose)
                .attempts((short) 0)
                .resendCount((short) 0)
                .status(OtpSessionStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}
