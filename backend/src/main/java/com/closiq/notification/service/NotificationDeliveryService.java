package com.closiq.notification.service;

import com.closiq.booking.domain.Booking;
import com.closiq.config.ClosiqProperties;
import com.closiq.identity.repository.UserRepository;
import com.closiq.notification.domain.NotificationCategory;
import com.closiq.notification.domain.NotificationChannel;
import com.closiq.notification.domain.NotificationType;
import com.closiq.notification.email.EmailService;
import com.closiq.notification.email.TransactionalEmailContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final NotificationPreferenceService preferenceService;
    private final NotificationChannelResolver channelResolver;
    private final ClosiqProperties closiqProperties;

    public void deliverBookingNotification(UUID userId, String notificationType, Booking booking, String deepLink) {
        CompletableFuture.runAsync(() -> deliverSafely(userId, notificationType, booking, deepLink));
    }

    private void deliverSafely(UUID userId, String notificationType, Booking booking, String deepLink) {
        try {
            NotificationCategory category = categoryFor(notificationType);
            attemptSms(userId, category, notificationType, booking.getRentalNumber());
            attemptPush(userId, category, notificationType);
            attemptEmail(userId, category, notificationType, booking, deepLink);
        } catch (Exception ex) {
            log.warn(
                    "Notification delivery failed for user {} type {} booking {}: {}",
                    userId,
                    notificationType,
                    booking.getRentalNumber(),
                    ex.getMessage());
        }
    }

    private void attemptEmail(
            UUID userId,
            NotificationCategory category,
            String notificationType,
            Booking booking,
            String deepLink) {

        if (!preferenceService.shouldAttemptDelivery(userId, NotificationChannel.EMAIL, category)) {
            if (!channelResolver.isAvailable(NotificationChannel.EMAIL)) {
                log.debug("Email skipped for {} — mail delivery not configured", notificationType);
            }
            return;
        }

        String email = userRepository.findByIdAndDeletedAtIsNull(userId)
                .map(user -> user.getEmail())
                .filter(value -> value != null && !value.isBlank())
                .orElse(null);
        if (email == null) {
            log.debug("Email skipped for user {} — no email on file", userId);
            return;
        }

        TransactionalEmailContext context = buildContext(booking, deepLink);
        switch (notificationType) {
            case NotificationType.BOOKING_CONFIRMED -> emailService.sendOrderConfirmed(email, context);
            case NotificationType.OUT_FOR_DELIVERY -> emailService.sendOutForDelivery(email, context);
            case NotificationType.RETURN_REMINDER -> emailService.sendReturnReminder(email, context);
            default -> {
                return;
            }
        }
    }

    private void attemptSms(UUID userId, NotificationCategory category, String notificationType, String rentalNumber) {
        if (!channelResolver.isAvailable(NotificationChannel.SMS)) {
            if (preferenceService.isChannelAllowed(userId, NotificationChannel.SMS, category)) {
                log.debug(
                        "SMS skipped for {} booking {} — SMS_PROVIDER_UNAVAILABLE",
                        notificationType,
                        rentalNumber);
            }
            return;
        }

        if (preferenceService.shouldAttemptDelivery(userId, NotificationChannel.SMS, category)) {
            log.info("SMS delivery not implemented for {} booking {}", notificationType, rentalNumber);
        }
    }

    private void attemptPush(UUID userId, NotificationCategory category, String notificationType) {
        if (!channelResolver.isAvailable(NotificationChannel.PUSH)) {
            return;
        }
        if (preferenceService.shouldAttemptDelivery(userId, NotificationChannel.PUSH, category)) {
            log.info("Push delivery not implemented for user {} type {}", userId, notificationType);
        }
    }

    private NotificationCategory categoryFor(String notificationType) {
        return switch (notificationType) {
            case NotificationType.RETURN_REMINDER -> NotificationCategory.RETURN_REMINDERS;
            case NotificationType.PROMOTION -> NotificationCategory.PROMOTIONS;
            case NotificationType.SELLER_NEW_BOOKING, NotificationType.SELLER_PAYOUT ->
                    NotificationCategory.SELLER_ALERTS;
            default -> NotificationCategory.ORDER_UPDATES;
        };
    }

    private TransactionalEmailContext buildContext(Booking booking, String deepLink) {
        String orderLabel = booking.getOrderNumber() != null
                ? "#" + booking.getOrderNumber()
                : booking.getRentalNumber();
        String actionUrl = absoluteUrl(deepLink);
        return TransactionalEmailContext.forBooking(
                orderLabel,
                booking.getRentalStartDate(),
                booking.getRentalEndDate(),
                booking.getTotalAmount(),
                booking.getDepositAmount(),
                actionUrl);
    }

    private String absoluteUrl(String deepLink) {
        String base = resolveAppBaseUrl();
        if (deepLink == null || deepLink.isBlank()) {
            return base;
        }
        if (deepLink.startsWith("http://") || deepLink.startsWith("https://")) {
            return deepLink;
        }
        String path = deepLink.startsWith("/") ? deepLink : "/" + deepLink;
        return base + path;
    }

    private String resolveAppBaseUrl() {
        String configured = closiqProperties.getNotification().getAppBaseUrl();
        if (configured != null && !configured.isBlank()) {
            return configured.replaceAll("/$", "");
        }
        if (!closiqProperties.getCors().getAllowedOrigins().isEmpty()) {
            return closiqProperties.getCors().getAllowedOrigins().getFirst().replaceAll("/$", "");
        }
        return "http://localhost:3000";
    }
}
