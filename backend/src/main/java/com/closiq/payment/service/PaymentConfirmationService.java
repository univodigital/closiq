package com.closiq.payment.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.domain.CheckoutBatch;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.repository.CheckoutBatchRepository;
import com.closiq.booking.repository.CheckoutSessionRepository;
import com.closiq.booking.service.BookingTimelineService;
import com.closiq.inventory.service.InventoryHoldService;
import com.closiq.notification.service.NotificationDispatchService;
import com.closiq.payment.domain.Payment;
import com.closiq.payment.domain.PaymentStatus;
import com.closiq.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentConfirmationService {

    private static final String COMPLETED_CHECKOUT = "COMPLETED";

    private final BookingRepository bookingRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final CheckoutBatchRepository checkoutBatchRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryHoldService inventoryHoldService;
    private final BookingTimelineService timelineService;
    private final NotificationDispatchService notificationDispatchService;

    @Transactional
    public Payment confirmPayment(Payment payment, String providerPaymentId, String paymentMethod) {
        Instant now = Instant.now();

        payment.setProviderPaymentId(providerPaymentId);
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setPaymentMethod(paymentMethod);
        payment.setCapturedAt(now);
        paymentRepository.save(payment);

        Booking booking = bookingRepository.findById(payment.getBookingId())
                .orElseThrow();

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(now);
        booking.setHoldExpiresAt(null);
        bookingRepository.save(booking);

        if (booking.getCheckoutSessionId() != null) {
            checkoutSessionRepository.findById(booking.getCheckoutSessionId()).ifPresent(session -> {
                session.setStatus(COMPLETED_CHECKOUT);
                checkoutSessionRepository.save(session);
            });
        }

        inventoryHoldService.confirmHold(booking.getId());
        timelineService.append(
                booking.getId(),
                payment.getCustomerId(),
                BookingStatus.CONFIRMED,
                "Payment captured — booking confirmed");

        notificationDispatchService.bookingConfirmed(booking);

        return payment;
    }

    @Transactional
    public Payment confirmBatchPayment(Payment payment, String providerPaymentId, String paymentMethod) {
        Instant now = Instant.now();

        payment.setProviderPaymentId(providerPaymentId);
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setPaymentMethod(paymentMethod);
        payment.setCapturedAt(now);
        paymentRepository.save(payment);

        UUID batchId = payment.getCheckoutBatchId();
        if (batchId == null) {
            throw new IllegalStateException("confirmBatchPayment requires checkoutBatchId");
        }

        List<Booking> bookings = bookingRepository.findByCheckoutBatchIdAndCustomerId(
                batchId, payment.getCustomerId());

        for (Booking booking : bookings) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setConfirmedAt(now);
            booking.setHoldExpiresAt(null);
            bookingRepository.save(booking);

            if (booking.getCheckoutSessionId() != null) {
                checkoutSessionRepository.findById(booking.getCheckoutSessionId()).ifPresent(session -> {
                    session.setStatus(COMPLETED_CHECKOUT);
                    checkoutSessionRepository.save(session);
                });
            }

            inventoryHoldService.confirmHold(booking.getId());
            timelineService.append(
                    booking.getId(),
                    payment.getCustomerId(),
                    BookingStatus.CONFIRMED,
                    "Payment captured — booking confirmed");
            notificationDispatchService.bookingConfirmed(booking);
        }

        checkoutBatchRepository.findById(batchId).ifPresent(batch -> {
            batch.setStatus(CheckoutBatch.COMPLETED);
            checkoutBatchRepository.save(batch);
        });

        return payment;
    }
}
