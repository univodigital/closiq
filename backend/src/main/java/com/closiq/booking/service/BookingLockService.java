package com.closiq.booking.service;

import com.closiq.config.ClosiqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingLockService {

    private final StringRedisTemplate redisTemplate;
    private final ClosiqProperties properties;

    public boolean tryAcquireVariantDateLock(UUID variantId, LocalDate startDate, LocalDate endDate) {
        String key = lockKey(variantId, startDate, endDate);
        Duration ttl = Duration.ofMinutes(properties.getBooking().getHoldTtlMinutes());
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(acquired);
    }

    public void releaseVariantDateLock(UUID variantId, LocalDate startDate, LocalDate endDate) {
        redisTemplate.delete(lockKey(variantId, startDate, endDate));
    }

    private String lockKey(UUID variantId, LocalDate startDate, LocalDate endDate) {
        return "booking:hold:" + variantId + ":" + startDate + ":" + endDate;
    }
}
