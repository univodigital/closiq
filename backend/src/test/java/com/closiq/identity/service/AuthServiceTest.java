package com.closiq.identity.service;

import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.security.JwtService;
import com.closiq.common.security.RoleType;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.util.HashUtils;
import com.closiq.config.ClosiqProperties;
import com.closiq.identity.domain.Gender;
import com.closiq.identity.domain.OtpPurpose;
import com.closiq.identity.domain.UserStatus;
import com.closiq.identity.domain.OtpSession;
import com.closiq.identity.domain.OtpSessionStatus;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserStatus;
import com.closiq.identity.mapper.UserMapper;
import com.closiq.identity.repository.OtpSessionRepository;
import com.closiq.identity.repository.PasswordResetTokenRepository;
import com.closiq.identity.repository.UserRepository;
import com.closiq.identity.web.dto.CompleteRegistrationRequest;
import com.closiq.identity.web.dto.VerifyOtpRequest;
import com.closiq.notification.email.EmailService;
import com.closiq.user.repository.SellerProfileRepository;
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
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OtpSessionRepository otpSessionRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private OtpService otpService;
    @Mock private OtpRateLimiter rateLimiter;
    @Mock private UserService userService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;
    @Mock private SellerProfileRepository sellerProfileRepository;
    @Mock private HashUtils hashUtils;
    @Mock private ClosiqProperties properties;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private UUID sessionId;
    private OtpSession session;
    private ClosiqProperties.Otp otpProps;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        session = OtpSession.builder()
                .id(sessionId)
                .phone("+919876543210")
                .otpHash("hashed-otp")
                .purpose(OtpPurpose.REGISTER)
                .attempts((short) 0)
                .resendCount((short) 0)
                .status(OtpSessionStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        otpProps = new ClosiqProperties.Otp();
        otpProps.setMaxVerifyAttempts(5);
        otpProps.setLockoutMinutes(15);
    }

    @Test
    void register_normalizesPhone() {
        when(userRepository.existsByPhoneAndPhoneVerifiedTrueAndDeletedAtIsNull("+919876543210"))
                .thenReturn(false);
        when(otpService.createSession(eq("+919876543210"), eq(OtpPurpose.REGISTER), eq(null)))
                .thenReturn(session);
        when(otpService.getExpirySeconds()).thenReturn(300);
        when(otpService.getResendCooldownRemainingSeconds(sessionId)).thenReturn(58);

        var response = authService.register("9876543210", null);

        assertThat(response.getPhone()).isEqualTo("+919876543210");
        assertThat(response.getResendAvailableInSeconds()).isEqualTo(58);
    }

    @Test
    void register_existingPhone_rejectedBeforeOtp() {
        when(userRepository.existsByPhoneAndPhoneVerifiedTrueAndDeletedAtIsNull("+919876543210"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register("+919876543210", "new@example.com"))
                .isInstanceOf(ClosiqException.class)
                .extracting(ex -> ((ClosiqException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_EXISTS);

        verify(otpService, never()).createSession(any(), any(), any());
    }

    @Test
    void verifyOtp_registerExistingUser_doesNotIssueTokens() {
        when(otpSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(hashUtils.hashOtp("123456")).thenReturn("hashed-otp");
        when(userRepository.findByPhoneAndDeletedAtIsNull("+919876543210"))
                .thenReturn(Optional.of(existingUser()));

        var result = authService.verifyOtp(
                new VerifyOtpRequest(sessionId.toString(), "123456", "REGISTER", null),
                "127.0.0.1",
                "test");

        assertThat(result.getRawRefreshToken()).isNull();
        assertThat(result.getResponse().getExistingAccount()).isTrue();
        assertThat(result.getResponse().getAccessToken()).isNull();
        verify(refreshTokenService, never()).issue(any(), any(), any());
    }

    @Test
    void verifyOtp_registerNewUserWithoutProfile_requiresProfile() {
        when(otpSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(hashUtils.hashOtp("123456")).thenReturn("hashed-otp");
        when(userRepository.findByPhoneAndDeletedAtIsNull("+919876543210")).thenReturn(Optional.empty());

        var result = authService.verifyOtp(
                new VerifyOtpRequest(sessionId.toString(), "123456", "REGISTER", null),
                "127.0.0.1",
                "test");

        assertThat(result.getResponse().getRequiresProfile()).isTrue();
        assertThat(result.getRawRefreshToken()).isNull();
    }

    @Test
    void verifyOtp_invalidOtp_rejected() {
        when(properties.getOtp()).thenReturn(otpProps);
        when(otpSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(hashUtils.hashOtp("000000")).thenReturn("wrong-hash");
        when(otpSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> authService.verifyOtp(
                new VerifyOtpRequest(sessionId.toString(), "000000", "REGISTER", null),
                "127.0.0.1",
                "test"))
                .isInstanceOf(ClosiqException.class)
                .extracting(ex -> ((ClosiqException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_OTP);
    }

    @Test
    void verifyOtp_expiredOtp_rejected() {
        session.setExpiresAt(Instant.now().minusSeconds(1));
        when(otpSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(otpSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> authService.verifyOtp(
                new VerifyOtpRequest(sessionId.toString(), "123456", "REGISTER", null),
                "127.0.0.1",
                "test"))
                .isInstanceOf(ClosiqException.class)
                .extracting(ex -> ((ClosiqException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_OTP);
    }

    @Test
    void completeRegistration_createsUserAfterVerifiedSession() {
        session.setStatus(OtpSessionStatus.VERIFIED);
        session.setVerifiedAt(Instant.now());
        var profile = new VerifyOtpRequest.RegistrationProfileRequest(
                "new_user", "Password1", "new@example.com", "Ana", "Sharma", Gender.FEMALE);
        User created = existingUser();

        when(otpSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(userRepository.findByPhoneAndDeletedAtIsNull("+919876543210")).thenReturn(Optional.empty());
        when(userService.usernameExists("new_user")).thenReturn(false);
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("new@example.com")).thenReturn(Optional.empty());
        when(hashUtils.hashPassword("Password1")).thenReturn("hash");
        when(userService.createUserWithUsername(
                eq("+919876543210"), eq("new_user"), eq("hash"), eq("new@example.com"),
                eq("Ana"), eq("Sharma"), eq(Gender.FEMALE))).thenReturn(created);
        when(userRepository.save(created)).thenReturn(created);
        when(userService.buildPrincipal(created)).thenReturn(new UserPrincipal(created.getId(), java.util.List.of(RoleType.CUSTOMER), true, null));
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.issue(eq(created), any(), any()))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken("refresh", null));
        when(userService.requireProfile(created.getId())).thenReturn(null);
        when(userService.getUserRoles(created.getId())).thenReturn(java.util.List.of(RoleType.CUSTOMER));
        when(userMapper.toSummaryWithRoleTypes(any(), any(), any())).thenReturn(null);
        when(sellerProfileRepository.findByUserId(created.getId())).thenReturn(Optional.empty());

        var result = authService.completeRegistration(
                new CompleteRegistrationRequest(sessionId.toString(), profile),
                "127.0.0.1",
                "test");

        assertThat(result.getAuth().getAccessToken()).isEqualTo("access-token");
        assertThat(result.getAuth().getIsNewUser()).isTrue();
    }

    @Test
    void completeRegistration_existingPhone_rejected() {
        session.setStatus(OtpSessionStatus.VERIFIED);
        session.setVerifiedAt(Instant.now());
        var profile = new VerifyOtpRequest.RegistrationProfileRequest(
                "new_user", "Password1", null, "Ana", "Sharma", Gender.FEMALE);

        when(otpSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(userRepository.findByPhoneAndDeletedAtIsNull("+919876543210"))
                .thenReturn(Optional.of(existingUser()));

        assertThatThrownBy(() -> authService.completeRegistration(
                new CompleteRegistrationRequest(sessionId.toString(), profile),
                "127.0.0.1",
                "test"))
                .isInstanceOf(ClosiqException.class)
                .extracting(ex -> ((ClosiqException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_EXISTS);
    }

    @Test
    void login_suspendedAccount_rejectedBeforeOtp() {
        when(userService.requireVerifiedUserForLogin(any()))
                .thenThrow(new ClosiqException(
                        ErrorCode.FORBIDDEN,
                        "Your account is suspended. Please contact support for assistance."));

        assertThatThrownBy(() -> authService.login("+919876543210"))
                .isInstanceOf(ClosiqException.class)
                .hasMessageContaining("suspended")
                .extracting(ex -> ((ClosiqException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(otpService, never()).createSession(any(), any(), any());
    }

    @Test
    void loginWithPassword_suspendedAccount_rejected() {
        when(userService.requireVerifiedUserForLogin(any()))
                .thenThrow(new ClosiqException(
                        ErrorCode.FORBIDDEN,
                        "Your account is suspended. Please contact support for assistance."));

        assertThatThrownBy(() -> authService.loginWithPassword(
                "+919876543210", "Password1", "127.0.0.1", "test"))
                .isInstanceOf(ClosiqException.class)
                .hasMessageContaining("suspended")
                .extracting(ex -> ((ClosiqException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void verifyOtp_loginSuspendedAccount_rejectedWithoutToken() {
        session.setPurpose(OtpPurpose.LOGIN);

        when(otpSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(hashUtils.hashOtp("123456")).thenReturn("hashed-otp");
        when(userRepository.findByPhoneAndDeletedAtIsNull("+919876543210"))
                .thenReturn(Optional.of(existingUser()));
        when(userService.requireLoginEligible(any()))
                .thenThrow(new ClosiqException(
                        ErrorCode.FORBIDDEN,
                        "Your account is suspended. Please contact support for assistance."));

        assertThatThrownBy(() -> authService.verifyOtp(
                new VerifyOtpRequest(sessionId.toString(), "123456", "LOGIN", null),
                "127.0.0.1",
                "test"))
                .isInstanceOf(ClosiqException.class)
                .hasMessageContaining("suspended");

        verify(refreshTokenService, never()).issue(any(), any(), any());
    }

    private User existingUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .phone("+919876543210")
                .phoneVerified(true)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
