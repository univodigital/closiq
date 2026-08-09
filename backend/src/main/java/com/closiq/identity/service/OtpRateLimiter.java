package com.closiq.identity.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpRateLimiter {

    private static final Duration OTP_SEND_WINDOW = Duration.ofMinutes(15);
    private static final Duration OTP_VERIFY_WINDOW = Duration.ofMinutes(15);
    private static final int OTP_SEND_LIMIT = 5;
    private static final int OTP_VERIFY_LIMIT = 10;

    private final StringRedisTemplate redisTemplate;
    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);
    private final ConcurrentHashMap<String, MemoryWindow> memoryWindows = new ConcurrentHashMap<>();

    public void checkSendAllowed(String phone) {
        checkLimit("otp:send:" + phone, OTP_SEND_LIMIT, OTP_SEND_WINDOW);
    }

    public void checkVerifyAllowed(String phone) {
        checkLimit("otp:verify:" + phone, OTP_VERIFY_LIMIT, OTP_VERIFY_WINDOW);
    }

    private void checkLimit(String key, int limit, Duration window) {
        if (redisAvailable.get() && tryRedisLimit(key, limit, window)) {
            return;
        }
        checkMemoryLimit(key, limit, window);
    }

    private boolean tryRedisLimit(String key, int limit, Duration window) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, window);
            }
            if (count != null && count > limit) {
                throw new ClosiqException(ErrorCode.RATE_LIMIT_EXCEEDED);
            }
            return true;
        } catch (ClosiqException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Redis rate limiter unavailable ({}), using in-memory fallback", ex.getMessage());
            redisAvailable.set(false);
            return false;
        }
    }

    private void checkMemoryLimit(String key, int limit, Duration window) {
        Instant now = Instant.now();
        MemoryWindow windowState = memoryWindows.compute(key, (k, existing) -> {
            if (existing == null || existing.expiresAt.isBefore(now)) {
                return new MemoryWindow(1, now.plus(window));
            }
            return new MemoryWindow(existing.count + 1, existing.expiresAt);
        });

        if (windowState.count > limit) {
            throw new ClosiqException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    private record MemoryWindow(int count, Instant expiresAt) {}
}
