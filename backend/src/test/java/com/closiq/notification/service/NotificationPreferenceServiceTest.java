package com.closiq.notification.service;

import com.closiq.config.ClosiqProperties;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.notification.domain.NotificationCategory;
import com.closiq.notification.domain.NotificationChannel;
import com.closiq.user.service.UserPreferencesHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    private UserPreferencesHelper preferencesHelper;
    private NotificationPreferenceService preferenceService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        preferencesHelper = new UserPreferencesHelper();
        ClosiqProperties properties = new ClosiqProperties();
        NotificationChannelResolver channelResolver = new NotificationChannelResolver(properties);
        preferenceService = new NotificationPreferenceService(
                userProfileRepository, preferencesHelper, channelResolver);
    }

    @Test
    void emailAllowedWhenEnabled() {
        stubPreferences(Map.of(
                "notifications",
                Map.of("emailEnabled", true, "orderUpdates", true)));

        assertThat(preferenceService.shouldAttemptDelivery(
                        userId, NotificationChannel.EMAIL, NotificationCategory.ORDER_UPDATES))
                .isTrue();
    }

    @Test
    void emailSkippedWhenUserDisabledEmail() {
        stubPreferences(Map.of(
                "notifications",
                Map.of("emailEnabled", false, "orderUpdates", true)));

        assertThat(preferenceService.shouldAttemptDelivery(
                        userId, NotificationChannel.EMAIL, NotificationCategory.ORDER_UPDATES))
                .isFalse();
    }

    @Test
    void smsNotAttemptedWhenProviderUnavailable() {
        assertThat(preferenceService.shouldAttemptDelivery(
                        userId, NotificationChannel.SMS, NotificationCategory.ORDER_UPDATES))
                .isFalse();
    }

    @Test
    void returnReminderRespectsCategoryToggle() {
        stubPreferences(Map.of(
                "notifications",
                Map.of("emailEnabled", true, "returnReminders", false)));

        assertThat(preferenceService.shouldAttemptDelivery(
                        userId, NotificationChannel.EMAIL, NotificationCategory.RETURN_REMINDERS))
                .isFalse();
    }

    private void stubPreferences(Map<String, Object> preferences) {
        UserProfile profile = UserProfile.builder().preferences(preferences).build();
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
    }
}
