package com.closiq.notification.service;

import com.closiq.booking.domain.Booking;
import com.closiq.common.util.IdGenerator;
import com.closiq.config.ClosiqProperties;
import com.closiq.identity.domain.User;
import com.closiq.identity.repository.UserRepository;
import com.closiq.notification.domain.NotificationType;
import com.closiq.notification.email.EmailService;
import com.closiq.notification.email.TransactionalEmailContext;
import com.closiq.notification.repository.NotificationRepository;
import com.closiq.user.repository.SellerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    private NotificationDeliveryService deliveryService;
    private NotificationDispatchService dispatchService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ClosiqProperties properties = new ClosiqProperties();
        NotificationChannelResolver channelResolver = new NotificationChannelResolver(properties);
        deliveryService = new NotificationDeliveryService(
                emailService, userRepository, preferenceService, channelResolver, properties);
        dispatchService = new NotificationDispatchService(
                notificationRepository, sellerProfileRepository, deliveryService);
    }

    @Test
    void bookingConfirmedTriggersEmailWhenPreferencesAllow() throws InterruptedException {
        Booking booking = sampleBooking();
        User user = User.builder().id(userId).email("user@example.com").build();

        when(notificationRepository.findByUserIdAndNotificationTypeOrderByCreatedAtDesc(
                        any(), any(), any()))
                .thenReturn(java.util.List.of());
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(preferenceService.shouldAttemptDelivery(any(), any(), any())).thenReturn(true);

        dispatchService.bookingConfirmed(booking);

        Thread.sleep(500);
        verify(emailService).sendOrderConfirmed(eq("user@example.com"), any(TransactionalEmailContext.class));
    }

    @Test
    void bookingConfirmedSkipsEmailWhenPreferencesDisallow() throws InterruptedException {
        Booking booking = sampleBooking();

        when(notificationRepository.findByUserIdAndNotificationTypeOrderByCreatedAtDesc(
                        any(), any(), any()))
                .thenReturn(java.util.List.of());
        when(preferenceService.shouldAttemptDelivery(any(), any(), any())).thenReturn(false);

        dispatchService.bookingConfirmed(booking);

        Thread.sleep(300);
        verify(emailService, never()).sendOrderConfirmed(any(), any());
    }

    @Test
    void duplicateInAppNotificationSkipsEmail() throws InterruptedException {
        Booking booking = sampleBooking();
        var existing = com.closiq.notification.domain.Notification.builder()
                .id(IdGenerator.uuidV7())
                .userId(userId)
                .notificationType(NotificationType.BOOKING_CONFIRMED)
                .payload(java.util.Map.of("bookingId", booking.getId().toString()))
                .build();

        when(notificationRepository.findByUserIdAndNotificationTypeOrderByCreatedAtDesc(
                        any(), any(), any()))
                .thenReturn(java.util.List.of(existing));

        dispatchService.bookingConfirmed(booking);

        verify(notificationRepository, never()).save(any());
        Thread.sleep(300);
        verify(emailService, never()).sendOrderConfirmed(any(), any());
    }

    private Booking sampleBooking() {
        return Booking.builder()
                .id(UUID.randomUUID())
                .customerId(userId)
                .rentalNumber("CLQ-1001")
                .orderNumber("ORD-1001")
                .status("CONFIRMED")
                .rentalStartDate(LocalDate.now().plusDays(2))
                .rentalEndDate(LocalDate.now().plusDays(5))
                .totalAmount(12_000)
                .depositAmount(5_000)
                .build();
    }
}
