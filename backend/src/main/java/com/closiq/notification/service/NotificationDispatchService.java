package com.closiq.notification.service;

import com.closiq.booking.domain.Booking;
import com.closiq.common.util.IdGenerator;
import com.closiq.notification.domain.Notification;
import com.closiq.notification.domain.NotificationType;
import com.closiq.notification.repository.NotificationRepository;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationRepository notificationRepository;
    private final SellerProfileRepository sellerProfileRepository;

    @Transactional
    public void bookingConfirmed(Booking booking) {
        send(
                booking.getCustomerId(),
                NotificationType.BOOKING_CONFIRMED,
                "Booking confirmed",
                "Your rental " + booking.getRentalNumber() + " is confirmed.",
                booking,
                orderDeepLink(booking));

        if (booking.getSellerProfileId() != null) {
            sellerProfileRepository.findById(booking.getSellerProfileId()).ifPresent(seller ->
                    send(
                            seller.getUser().getId(),
                            NotificationType.SELLER_NEW_BOOKING,
                            "New booking received",
                            "You have a new booking " + booking.getRentalNumber() + " to accept.",
                            booking,
                            "/seller/bookings/" + booking.getId()));
        }
    }

    @Transactional
    public void outForDelivery(Booking booking) {
        send(
                booking.getCustomerId(),
                NotificationType.OUT_FOR_DELIVERY,
                "Out for delivery",
                "Your order is on the way for booking " + booking.getRentalNumber() + ".",
                booking,
                orderDeepLink(booking));
    }

    @Transactional
    public void trialReady(Booking booking) {
        send(
                booking.getCustomerId(),
                NotificationType.TRIAL_READY,
                "Your trial is ready",
                "The delivery agent has arrived for booking " + booking.getRentalNumber() + ".",
                booking,
                orderDeepLink(booking));
    }

    @Transactional
    public void returnScheduled(Booking booking) {
        send(
                booking.getCustomerId(),
                NotificationType.RETURN_SCHEDULED,
                "Return pickup scheduled",
                "Return pickup is scheduled for booking " + booking.getRentalNumber() + ".",
                booking,
                orderDeepLink(booking));
    }

    @Transactional
    public void sellerPayout(SellerProfile seller, long amountPaise, UUID payoutId) {
        send(
                seller.getUser().getId(),
                NotificationType.SELLER_PAYOUT,
                "Payout initiated",
                "Your payout of ₹" + formatRupees(amountPaise) + " is being processed.",
                Map.of("payoutId", payoutId.toString(), "amount", amountPaise),
                "/seller/wallet");
    }

    @Transactional
    public void depositRefunded(Booking booking, long amountPaise) {
        send(
                booking.getCustomerId(),
                NotificationType.DEPOSIT_REFUNDED,
                "Deposit refunded",
                "Your deposit of ₹" + formatRupees(amountPaise) + " has been refunded.",
                booking,
                orderDeepLink(booking));
    }

    private void send(
            UUID userId,
            String type,
            String title,
            String body,
            Booking booking,
            String deepLink) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", booking.getId().toString());
        payload.put("rentalNumber", booking.getRentalNumber());
        payload.put("bookingNumber", booking.getRentalNumber());
        if (booking.getOrderNumber() != null) {
            payload.put("orderNumber", booking.getOrderNumber());
        }
        payload.put("status", booking.getStatus());

        persist(userId, type, title, body, payload, deepLink);
    }

    private void send(
            UUID userId,
            String type,
            String title,
            String body,
            Map<String, Object> payload,
            String deepLink) {

        persist(userId, type, title, body, payload, deepLink);
    }

    private void persist(
            UUID userId,
            String type,
            String title,
            String body,
            Map<String, Object> payload,
            String deepLink) {

        notificationRepository.save(Notification.builder()
                .id(IdGenerator.uuidV7())
                .userId(userId)
                .notificationType(type)
                .title(title)
                .body(body)
                .payload(payload)
                .deepLink(deepLink)
                .read(false)
                .build());
    }

    private String orderDeepLink(Booking booking) {
        if (booking.getOrderNumber() != null) {
            return "/orders/" + booking.getOrderNumber();
        }
        return "/bookings/" + booking.getRentalNumber();
    }

    private String formatRupees(long amountPaise) {
        return String.format("%.2f", amountPaise / 100.0);
    }
}
