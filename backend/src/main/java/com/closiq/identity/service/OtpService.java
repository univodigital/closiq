package com.closiq.identity.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.HashUtils;
import com.closiq.common.util.IdGenerator;
import com.closiq.config.ClosiqProperties;
import com.closiq.identity.domain.OtpPurpose;
import com.closiq.identity.domain.OtpSession;
import com.closiq.identity.domain.OtpSessionStatus;
import com.closiq.identity.repository.OtpSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
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
        markResendCooldown(session.getId());
        try {
            otpSender.sendOtp(phone, email, otp, purpose.name());
        } catch (Exception ex) {
            log.warn("OTP delivery failed for {} ({}): {}", phone, purpose, ex.getMessage());
        }
        return session;
    }

    @Transactional
    public OtpSession resendSession(UUID sessionId) {
        OtpSession session = otpSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.INVALID_OTP, "Invalid OTP session"));

        if (session.getStatus() != OtpSessionStatus.PENDING) {
            throw new ClosiqException(ErrorCode.INVALID_OTP, "OTP session is no longer valid");
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setStatus(OtpSessionStatus.EXPIRED);
            otpSessionRepository.save(session);
            throw new ClosiqException(ErrorCode.INVALID_OTP, "OTP has expired");
        }

        if (session.getResendCount() >= properties.getOtp().getMaxResendsPerSession()) {
            throw new ClosiqException(ErrorCode.RATE_LIMIT_EXCEEDED, "Maximum resend attempts reached");
        }

        rateLimiter.checkResendCooldown(sessionId.toString(),
                Duration.ofSeconds(properties.getOtp().getResendCooldownSeconds()));
        rateLimiter.checkSendAllowed(session.getPhone());

        String otp = hashUtils.generateNumericOtp(properties.getOtp().getLength());
        session.setOtpHash(hashUtils.hashOtp(otp));
        session.setExpiresAt(Instant.now().plusSeconds(properties.getOtp().getExpirySeconds()));
        session.setResendCount((short) (session.getResendCount() + 1));
        session.setAttempts((short) 0);
        session.setLockedUntil(null);
        otpSessionRepository.save(session);
        markResendCooldown(session.getId());

        try {
            otpSender.sendOtp(session.getPhone(), null, otp, session.getPurpose().name());
        } catch (Exception ex) {
            log.warn("OTP resend delivery failed for {} ({}): {}",
                    session.getPhone(), session.getPurpose(), ex.getMessage());
        }

        return session;
    }

    public int getResendCooldownRemainingSeconds(UUID sessionId) {
        return rateLimiter.getResendCooldownRemainingSeconds(sessionId.toString());
    }

    private void markResendCooldown(UUID sessionId) {
        rateLimiter.setResendCooldown(
                sessionId.toString(),
                Duration.ofSeconds(properties.getOtp().getResendCooldownSeconds()));
    }

    public int getExpirySeconds() {
        return properties.getOtp().getExpirySeconds();
    }

    public int getResendCooldownSeconds() {
        return properties.getOtp().getResendCooldownSeconds();
    }
}
