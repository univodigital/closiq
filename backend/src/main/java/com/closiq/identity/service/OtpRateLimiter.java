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

    public void setResendCooldown(String sessionId, Duration cooldown) {
        String key = resendCooldownKey(sessionId);
        if (redisAvailable.get() && trySetRedisCooldown(key, cooldown)) {
            return;
        }
        memoryWindows.put(key, new MemoryWindow(1, Instant.now().plus(cooldown)));
    }

    public void checkResendCooldown(String sessionId, Duration cooldown) {
        String key = resendCooldownKey(sessionId);
        if (redisAvailable.get()) {
            try {
                Long ttl = redisTemplate.getExpire(key);
                if (ttl != null && ttl > 0) {
                    throw resendCooldownException(ttl);
                }
                return;
            } catch (ClosiqException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("Redis resend cooldown unavailable ({}), using in-memory fallback", ex.getMessage());
                redisAvailable.set(false);
            }
        }
        MemoryWindow window = memoryWindows.get(key);
        if (window != null && window.expiresAt.isAfter(Instant.now())) {
            long seconds = Duration.between(Instant.now(), window.expiresAt).getSeconds();
            throw resendCooldownException(Math.max(seconds, 1));
        }
    }

    public int getResendCooldownRemainingSeconds(String sessionId) {
        String key = resendCooldownKey(sessionId);
        if (redisAvailable.get()) {
            try {
                Long ttl = redisTemplate.getExpire(key);
                if (ttl != null && ttl > 0) {
                    return ttl.intValue();
                }
                return 0;
            } catch (Exception ex) {
                redisAvailable.set(false);
            }
        }
        MemoryWindow window = memoryWindows.get(key);
        if (window != null && window.expiresAt.isAfter(Instant.now())) {
            return (int) Math.max(Duration.between(Instant.now(), window.expiresAt).getSeconds(), 1);
        }
        return 0;
    }

    private static String resendCooldownKey(String sessionId) {
        return "otp:resend:cooldown:" + sessionId;
    }

    private boolean trySetRedisCooldown(String key, Duration cooldown) {
        try {
            redisTemplate.opsForValue().set(key, "1", cooldown);
            return true;
        } catch (Exception ex) {
            log.warn("Redis resend cooldown set failed ({}), using in-memory fallback", ex.getMessage());
            redisAvailable.set(false);
            return false;
        }
    }

    private ClosiqException resendCooldownException(long remainingSeconds) {
        ClosiqException ex = new ClosiqException(ErrorCode.RATE_LIMIT_EXCEEDED,
                "Resend available in " + remainingSeconds + " seconds");
        return ex;
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
