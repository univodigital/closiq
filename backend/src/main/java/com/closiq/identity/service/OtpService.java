package com.closiq.identity.service;

import com.closiq.common.util.HashUtils;
import com.closiq.common.util.IdGenerator;
import com.closiq.config.ClosiqProperties;
import com.closiq.identity.domain.OtpPurpose;
import com.closiq.identity.domain.OtpSession;
import com.closiq.identity.domain.OtpSessionStatus;
import com.closiq.identity.repository.OtpSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpSessionRepository otpSessionRepository;
    private final HashUtils hashUtils;
    private final OtpSender otpSender;
    private final OtpRateLimiter rateLimiter;
    private final ClosiqProperties properties;

    @Transactional
    public OtpSession createSession(String phone, OtpPurpose purpose) {
        return createSession(phone, purpose, null);
    }

    @Transactional
    public OtpSession createSession(String phone, OtpPurpose purpose, String email) {
        rateLimiter.checkSendAllowed(phone);

        String otp = hashUtils.generateNumericOtp(properties.getOtp().getLength());
        Instant expiresAt = Instant.now().plusSeconds(properties.getOtp().getExpirySeconds());

        OtpSession session = OtpSession.builder()
                .id(IdGenerator.uuidV7())
                .phone(phone)
                .otpHash(hashUtils.hashOtp(otp))
                .purpose(purpose)
                .attempts((short) 0)
                .resendCount((short) 0)
                .status(OtpSessionStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        otpSessionRepository.save(session);
        otpSender.sendOtp(phone, email, otp, purpose.name());
        return session;
    }

    public int getExpirySeconds() {
        return properties.getOtp().getExpirySeconds();
    }

    public int getResendCooldownSeconds() {
        return properties.getOtp().getResendCooldownSeconds();
    }
}
