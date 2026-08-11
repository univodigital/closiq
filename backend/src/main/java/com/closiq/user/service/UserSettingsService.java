package com.closiq.user.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.identity.service.UserService;
import com.closiq.notification.domain.NotificationChannel;
import com.closiq.notification.service.NotificationChannelResolver;
import com.closiq.user.domain.ServiceablePincode;
import com.closiq.user.mapper.UserProfileMapper;
import com.closiq.user.repository.ServiceablePincodeRepository;
import com.closiq.user.web.dto.AccountSettingsResponse;
import com.closiq.user.web.dto.NotificationPreferencesResponse;
import com.closiq.user.web.dto.PincodeServiceabilityResponse;
import com.closiq.user.web.dto.UpdateAccountSettingsRequest;
import com.closiq.user.web.dto.UpdateNotificationPreferencesRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private static final String ACTIVE_PINCODE = "ACTIVE";

    private final UserProfileRepository userProfileRepository;
    private final ServiceablePincodeRepository serviceablePincodeRepository;
    private final UserService userService;
    private final UserProfileMapper userProfileMapper;
    private final UserPreferencesHelper preferencesHelper;
    private final NotificationChannelResolver channelResolver;

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse getNotificationPreferences(UUID userId) {
        UserProfile profile = userService.requireProfile(userId);
        return withChannelAvailability(userProfileMapper.toNotificationResponse(profile.getPreferences()));
    }

    @Transactional
    public NotificationPreferencesResponse updateNotificationPreferences(
            UUID userId, UpdateNotificationPreferencesRequest request) {

        UserProfile profile = userService.requireProfile(userId);
        UserPreferencesHelper.NotificationPreferences current =
                preferencesHelper.getNotifications(profile.getPreferences());

        boolean smsEnabled = request.getSmsEnabled() != null ? request.getSmsEnabled() : current.smsEnabled();
        if (smsEnabled && !channelResolver.isAvailable(NotificationChannel.SMS)) {
            smsEnabled = false;
        }
        boolean pushEnabled = request.getPushEnabled() != null ? request.getPushEnabled() : current.pushEnabled();
        if (pushEnabled && !channelResolver.isAvailable(NotificationChannel.PUSH)) {
            pushEnabled = false;
        }

        UserPreferencesHelper.NotificationPreferences updated = new UserPreferencesHelper.NotificationPreferences(
                request.getEmailEnabled() != null ? request.getEmailEnabled() : current.emailEnabled(),
                smsEnabled,
                pushEnabled,
                request.getOrderUpdates() != null ? request.getOrderUpdates() : current.orderUpdates(),
                request.getReturnReminders() != null ? request.getReturnReminders() : current.returnReminders(),
                request.getPromotions() != null ? request.getPromotions() : current.promotions(),
                request.getSellerBookingAlerts() != null
                        ? request.getSellerBookingAlerts()
                        : current.sellerBookingAlerts());

        Map<String, Object> preferences = preferencesHelper.mergeNotifications(
                profile.getPreferences(), updated);
        profile.setPreferences(preferences);
        userProfileRepository.save(profile);

        return withChannelAvailability(userProfileMapper.toNotificationResponse(preferences));
    }

    private NotificationPreferencesResponse withChannelAvailability(NotificationPreferencesResponse prefs) {
        return NotificationPreferencesResponse.builder()
                .emailEnabled(prefs.isEmailEnabled())
                .smsEnabled(prefs.isSmsEnabled())
                .pushEnabled(prefs.isPushEnabled())
                .orderUpdates(prefs.isOrderUpdates())
                .returnReminders(prefs.isReturnReminders())
                .promotions(prefs.isPromotions())
                .sellerBookingAlerts(prefs.isSellerBookingAlerts())
                .emailAvailable(channelResolver.isAvailable(NotificationChannel.EMAIL))
                .smsAvailable(channelResolver.isAvailable(NotificationChannel.SMS))
                .pushAvailable(channelResolver.isAvailable(NotificationChannel.PUSH))
                .build();
    }

    @Transactional(readOnly = true)
    public AccountSettingsResponse getAccountSettings(UUID userId) {
        UserProfile profile = userService.requireProfile(userId);
        var account = preferencesHelper.getAccount(profile.getPreferences());
        return AccountSettingsResponse.builder()
                .language(account.language())
                .marketingOptIn(account.marketingOptIn())
                .build();
    }

    @Transactional
    public AccountSettingsResponse updateAccountSettings(UUID userId, UpdateAccountSettingsRequest request) {
        UserProfile profile = userService.requireProfile(userId);
        var current = preferencesHelper.getAccount(profile.getPreferences());

        var updated = new UserPreferencesHelper.AccountSettings(
                request.getLanguage() != null ? request.getLanguage() : current.language(),
                request.getMarketingOptIn() != null ? request.getMarketingOptIn() : current.marketingOptIn());

        Map<String, Object> preferences = preferencesHelper.mergeAccount(profile.getPreferences(), updated);
        profile.setPreferences(preferences);
        userProfileRepository.save(profile);

        return AccountSettingsResponse.builder()
                .language(updated.language())
                .marketingOptIn(updated.marketingOptIn())
                .build();
    }

    @Transactional(readOnly = true)
    public PincodeServiceabilityResponse checkPincode(String pincode) {
        if (pincode == null || !pincode.matches("\\d{6}")) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Pincode must be 6 digits");
        }

        return serviceablePincodeRepository.findByPincodeAndStatus(pincode, ACTIVE_PINCODE)
                .map(this::toServiceabilityResponse)
                .orElseGet(() -> PincodeServiceabilityResponse.builder()
                        .pincode(pincode)
                        .serviceable(false)
                        .city(null)
                        .state(null)
                        .estimatedDeliveryDays(0)
                        .launchPhase(null)
                        .build());
    }

    private PincodeServiceabilityResponse toServiceabilityResponse(ServiceablePincode pincode) {
        return PincodeServiceabilityResponse.builder()
                .pincode(pincode.getPincode())
                .serviceable(true)
                .city(pincode.getCity())
                .state(pincode.getState())
                .estimatedDeliveryDays(pincode.getEstimatedDeliveryDays())
                .launchPhase(pincode.getLaunchPhase())
                .build();
    }
}
