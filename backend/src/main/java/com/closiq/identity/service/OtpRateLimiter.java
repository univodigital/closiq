package com.closiq.identity.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OtpRateLimiter {

    private static final Duration OTP_SEND_WINDOW = Duration.ofMinutes(15);
    private static final Duration OTP_VERIFY_WINDOW = Duration.ofMinutes(15);
    private static final int OTP_SEND_LIMIT = 5;
    private static final int OTP_VERIFY_LIMIT = 10;

    private final StringRedisTemplate redisTemplate;

    public void checkSendAllowed(String phone) {
        checkLimit("otp:send:" + phone, OTP_SEND_LIMIT, OTP_SEND_WINDOW);
    }

    public void checkVerifyAllowed(String phone) {
        checkLimit("otp:verify:" + phone, OTP_VERIFY_LIMIT, OTP_VERIFY_WINDOW);
    }

    private void checkLimit(String key, int limit, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        if (count != null && count > limit) {
            throw new ClosiqException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }
}
