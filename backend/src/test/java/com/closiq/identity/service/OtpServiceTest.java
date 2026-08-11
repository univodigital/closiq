package com.closiq.identity.service;

import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.util.HashUtils;
import com.closiq.config.ClosiqProperties;
import com.closiq.identity.domain.OtpPurpose;
import com.closiq.identity.domain.OtpSession;
import com.closiq.identity.domain.OtpSessionStatus;
import com.closiq.identity.repository.OtpSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpSessionRepository otpSessionRepository;
    @Mock
    private HashUtils hashUtils;
    @Mock
    private OtpSender otpSender;
    @Mock
    private OtpRateLimiter rateLimiter;
    @Mock
    private ClosiqProperties properties;

    @InjectMocks
    private OtpService otpService;

    @Test
    void createSession_persistsOtpAndSendsSms() {
        ClosiqProperties.Otp otp = new ClosiqProperties.Otp();
        otp.setLength(6);
        otp.setExpirySeconds(300);
        when(properties.getOtp()).thenReturn(otp);
        when(hashUtils.generateNumericOtp(6)).thenReturn("123456");
        when(hashUtils.hashOtp("123456")).thenReturn("hashed-otp");
        when(otpSessionRepository.save(any(OtpSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpSession session = otpService.createSession("+919876543210", OtpPurpose.REGISTER);

        assertThat(session.getPhone()).isEqualTo("+919876543210");
        assertThat(session.getPurpose()).isEqualTo(OtpPurpose.REGISTER);
        assertThat(session.getOtpHash()).isEqualTo("hashed-otp");
        assertThat(session.getExpiresAt()).isAfter(Instant.now());

        verify(rateLimiter).checkSendAllowed("+919876543210");
        verify(otpSender).sendOtp("+919876543210", null, "123456", "REGISTER");

        ArgumentCaptor<OtpSession> captor = ArgumentCaptor.forClass(OtpSession.class);
        verify(otpSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OtpSessionStatus.PENDING);
    }

    @Test
    void resendSession_generatesNewOtpAndIncrementsResendCount() {
        UUID sessionId = UUID.randomUUID();
        OtpSession existing = OtpSession.builder()
                .id(sessionId)
                .phone("+919876543210")
                .otpHash("old-hash")
                .purpose(OtpPurpose.REGISTER)
                .attempts((short) 2)
                .resendCount((short) 0)
                .status(OtpSessionStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(120))
                .build();

        ClosiqProperties.Otp otp = new ClosiqProperties.Otp();
        otp.setLength(6);
        otp.setExpirySeconds(300);
        otp.setResendCooldownSeconds(60);
        otp.setMaxResendsPerSession(3);
        when(properties.getOtp()).thenReturn(otp);
        when(otpSessionRepository.findById(sessionId)).thenReturn(Optional.of(existing));
        when(hashUtils.generateNumericOtp(6)).thenReturn("654321");
        when(hashUtils.hashOtp("654321")).thenReturn("new-hash");
        when(otpSessionRepository.save(any(OtpSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpSession resent = otpService.resendSession(sessionId);

        assertThat(resent.getOtpHash()).isEqualTo("new-hash");
        assertThat(resent.getResendCount()).isEqualTo((short) 1);
        assertThat(resent.getAttempts()).isEqualTo((short) 0);
        verify(otpSender).sendOtp("+919876543210", null, "654321", "REGISTER");
    }

    @Test
    void resendSession_maxResends_rejected() {
        UUID sessionId = UUID.randomUUID();
        OtpSession existing = OtpSession.builder()
                .id(sessionId)
                .phone("+919876543210")
                .otpHash("old-hash")
                .purpose(OtpPurpose.REGISTER)
                .attempts((short) 0)
                .resendCount((short) 3)
                .status(OtpSessionStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(120))
                .build();

        ClosiqProperties.Otp otp = new ClosiqProperties.Otp();
        otp.setMaxResendsPerSession(3);
        when(properties.getOtp()).thenReturn(otp);
        when(otpSessionRepository.findById(sessionId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> otpService.resendSession(sessionId))
                .isInstanceOf(ClosiqException.class)
                .extracting(ex -> ((ClosiqException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
    }
}
