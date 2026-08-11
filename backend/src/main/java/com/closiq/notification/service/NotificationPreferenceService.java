package com.closiq.notification.service;

import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.notification.domain.NotificationCategory;
import com.closiq.notification.domain.NotificationChannel;
import com.closiq.user.service.UserPreferencesHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final UserProfileRepository userProfileRepository;
    private final UserPreferencesHelper preferencesHelper;
    private final NotificationChannelResolver channelResolver;

    public UserPreferencesHelper.NotificationPreferences getPreferences(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .map(profile -> preferencesHelper.getNotifications(profile.getPreferences()))
                .orElseGet(() -> preferencesHelper.getNotifications(Map.of()));
    }

    public boolean isChannelAllowed(
            UUID userId, NotificationChannel channel, NotificationCategory category) {

        UserPreferencesHelper.NotificationPreferences prefs = getPreferences(userId);

        return switch (channel) {
            case EMAIL -> prefs.emailEnabled() && isCategoryEnabled(prefs, category);
            case SMS -> prefs.smsEnabled() && isCategoryEnabled(prefs, category);
            case PUSH -> prefs.pushEnabled() && isCategoryEnabled(prefs, category);
        };
    }

    public boolean shouldAttemptDelivery(
            UUID userId, NotificationChannel channel, NotificationCategory category) {

        if (!channelResolver.isAvailable(channel)) {
            return false;
        }
        return isChannelAllowed(userId, channel, category);
    }

    private boolean isCategoryEnabled(
            UserPreferencesHelper.NotificationPreferences prefs, NotificationCategory category) {

        return switch (category) {
            case ORDER_UPDATES -> prefs.orderUpdates();
            case RETURN_REMINDERS -> prefs.returnReminders();
            case PROMOTIONS -> prefs.promotions();
            case SELLER_ALERTS -> prefs.sellerBookingAlerts();
        };
    }
}
