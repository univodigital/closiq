package com.closiq.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountChangeStateService {

    private static final Duration STATE_TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public record PhoneChangeState(boolean oldPhoneVerified, String newPhone) {
    }

    public record EmailChangeState(String pendingEmail) {
    }

    public void markOldPhoneVerified(UUID userId) {
        PhoneChangeState state = new PhoneChangeState(true, null);
        writePhoneState(userId, state);
    }

    public void setPendingNewPhone(UUID userId, String newPhone) {
        PhoneChangeState current = readPhoneState(userId).orElse(new PhoneChangeState(true, null));
        if (!current.oldPhoneVerified()) {
            throw new IllegalStateException("Old phone must be verified first");
        }
        writePhoneState(userId, new PhoneChangeState(true, newPhone));
    }

    public Optional<PhoneChangeState> getPhoneChangeState(UUID userId) {
        return readPhoneState(userId);
    }

    public void clearPhoneChangeState(UUID userId) {
        redisTemplate.delete(phoneKey(userId));
    }

    public void storeEmailChangeSession(UUID sessionId, UUID userId, String pendingEmail) {
        try {
            String payload = objectMapper.writeValueAsString(new EmailChangeState(pendingEmail));
            redisTemplate.opsForValue().set(emailSessionKey(sessionId), payload, STATE_TTL);
            redisTemplate.opsForValue().set(emailUserKey(userId), sessionId.toString(), STATE_TTL);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to store email change state", ex);
        }
    }

    public Optional<EmailChangeState> getEmailChangeSession(UUID sessionId) {
        String raw = redisTemplate.opsForValue().get(emailSessionKey(sessionId));
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, EmailChangeState.class));
        } catch (JsonProcessingException ex) {
            log.warn("Invalid email change state for session {}", sessionId);
            return Optional.empty();
        }
    }

    public void clearEmailChangeState(UUID userId, UUID sessionId) {
        redisTemplate.delete(emailSessionKey(sessionId));
        redisTemplate.delete(emailUserKey(userId));
    }

    private void writePhoneState(UUID userId, PhoneChangeState state) {
        try {
            redisTemplate.opsForValue().set(
                    phoneKey(userId),
                    objectMapper.writeValueAsString(state),
                    STATE_TTL);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to store phone change state", ex);
        }
    }

    private Optional<PhoneChangeState> readPhoneState(UUID userId) {
        String raw = redisTemplate.opsForValue().get(phoneKey(userId));
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, PhoneChangeState.class));
        } catch (JsonProcessingException ex) {
            log.warn("Invalid phone change state for user {}", userId);
            return Optional.empty();
        }
    }

    private static String phoneKey(UUID userId) {
        return "account:phone-change:" + userId;
    }

    private static String emailSessionKey(UUID sessionId) {
        return "account:email-change:session:" + sessionId;
    }

    private static String emailUserKey(UUID userId) {
        return "account:email-change:user:" + userId;
    }
}
