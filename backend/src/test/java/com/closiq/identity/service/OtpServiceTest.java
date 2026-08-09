package com.closiq.identity.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.HashUtils;
import com.closiq.config.ClosiqProperties;
import com.closiq.identity.domain.OtpPurpose;
import com.closiq.identity.domain.OtpSession;
import com.closiq.identity.domain.OtpSessionStatus;
import com.closiq.identity.repository.OtpSessionRepository;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
        ClosiqProperties.Otp otp = new ClosiqProperties.Otp();
        otp.setLength(6);
        otp.setExpirySeconds(300);
        when(properties.getOtp()).thenReturn(otp);
        when(hashUtils.generateNumericOtp(6)).thenReturn("123456");
        when(hashUtils.hashOtp("123456")).thenReturn("hashed-otp");
    }

    @Test
    void createSession_persistsOtpAndSendsSms() {
        when(otpSessionRepository.save(any(OtpSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpSession session = otpService.createSession("+919876543210", OtpPurpose.REGISTER);

        assertThat(session.getPhone()).isEqualTo("+919876543210");
        assertThat(session.getPurpose()).isEqualTo(OtpPurpose.REGISTER);
        assertThat(session.getOtpHash()).isEqualTo("hashed-otp");
        assertThat(session.getExpiresAt()).isAfter(Instant.now());

        verify(rateLimiter).checkSendAllowed("+919876543210");
        verify(otpSender).sendOtp("+919876543210", "123456", "REGISTER");

        ArgumentCaptor<OtpSession> captor = ArgumentCaptor.forClass(OtpSession.class);
        verify(otpSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OtpSessionStatus.PENDING);
    }
}
