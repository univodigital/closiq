package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.domain.TrialSession;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.repository.TrialSessionRepository;
import com.closiq.booking.web.dto.TrialRejectRequest;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.util.IdGenerator;
import com.closiq.notification.service.NotificationDispatchService;
import com.closiq.payment.domain.Payment;
import com.closiq.payment.domain.PaymentStatus;
import com.closiq.payment.repository.PaymentRepository;
import com.closiq.payment.service.RefundService;
import com.closiq.shipment.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingLifecycleServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private TrialSessionRepository trialSessionRepository;
    @Mock private BookingTimelineService timelineService;
    @Mock private NotificationDispatchService notificationDispatchService;
    @Mock private RefundService refundService;
    @Mock private TrialPolicyService trialPolicyService;
    @Mock private ShipmentService shipmentService;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private BookingLifecycleService lifecycleService;

    private UUID customerId;
    private UUID bookingId;
    private Booking booking;
    private TrialSession trialSession;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        booking = Booking.builder()
                .id(bookingId)
                .customerId(customerId)
                .status(BookingStatus.TRIAL_READY)
                .rentalNumber("BK-2026-000001")
                .rentalAmount(3000)
                .discountAmount(0)
                .depositAmount(2000)
                .deliveryFee(0)
                .trialDurationMinutes((short) 15)
                .build();

        Instant started = Instant.now().minusSeconds(60);
        trialSession = TrialSession.builder()
                .id(IdGenerator.uuidV7())
                .bookingId(bookingId)
                .startedAt(started)
                .expiresAt(started.plusSeconds(15 * 60L))
                .outcome("PENDING")
                .build();
    }

    @Test
    void acceptTrial_withinWindow_transitionsToRentalActive() {
        when(bookingRepository.findByIdAndCustomerId(bookingId, customerId)).thenReturn(Optional.of(booking));
        when(trialSessionRepository.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.of(trialSession));

        var result = lifecycleService.acceptTrial(customerId, bookingId.toString());

        assertThat(result.get("status")).isEqualTo(BookingStatus.RENTAL_ACTIVE);
        assertThat(trialSession.getOutcome()).isEqualTo("ACCEPTED");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RENTAL_ACTIVE);
        verify(notificationDispatchService).trialAccepted(booking);
    }

    @Test
    void acceptTrial_afterExpiration_isRejected() {
        trialSession.setExpiresAt(Instant.now().minusSeconds(30));
        when(bookingRepository.findByIdAndCustomerId(bookingId, customerId)).thenReturn(Optional.of(booking));
        when(trialSessionRepository.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.of(trialSession));

        assertThatThrownBy(() -> lifecycleService.acceptTrial(customerId, bookingId.toString()))
                .isInstanceOf(ClosiqException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rejectTrial_afterAcceptanceAlreadyCompleted_fails() {
        trialSession.setOutcome("ACCEPTED");
        when(bookingRepository.findByIdAndCustomerId(bookingId, customerId)).thenReturn(Optional.of(booking));
        when(trialSessionRepository.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.of(trialSession));

        assertThatThrownBy(() -> lifecycleService.rejectTrial(
                        customerId, bookingId.toString(), new TrialRejectRequest("CHANGED_MIND", null)))
                .isInstanceOf(ClosiqException.class)
                .hasMessageContaining("already been completed");
    }

    @Test
    void ensureTrialSession_usesBookingDuration() {
        when(trialSessionRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(trialSessionRepository.save(any(TrialSession.class))).thenAnswer(inv -> inv.getArgument(0));

        TrialSession created = lifecycleService.ensureTrialSession(bookingId);

        assertThat(created.getStartedAt()).isNotNull();
        assertThat(created.getExpiresAt()).isAfter(created.getStartedAt());
        assertThat(created.getOutcome()).isEqualTo("PENDING");
    }

    @Test
    void rejectTrial_initiatesRentalRefundOnly() {
        when(bookingRepository.findByIdAndCustomerId(bookingId, customerId)).thenReturn(Optional.of(booking));
        when(trialSessionRepository.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.of(trialSession));
        when(trialPolicyService.previewReject(booking)).thenReturn(
                com.closiq.booking.web.dto.TrialRejectPreviewResponse.builder()
                        .rentalRefundAmount(3000)
                        .depositRefundAmount(0)
                        .rentalRefundExpectedBusinessDays(5)
                        .build());

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.CAPTURED)
                .amount(500000)
                .providerPaymentId("pay_test")
                .build();
        when(refundService.resolvePaymentForBooking(booking)).thenReturn(payment);

        lifecycleService.rejectTrial(customerId, bookingId.toString(), new TrialRejectRequest("CHANGED_MIND", null));

        verify(refundService).initiateRefund(
                eq(payment.getId()),
                eq(bookingId),
                eq(RefundService.TYPE_RENTAL),
                eq(300000L),
                eq("trial-reject-rental-" + bookingId),
                eq(customerId),
                any(),
                eq(5));
        verify(notificationDispatchService).trialRejected(booking);
        verify(refundService, never()).initiateBookingRefund(any(), any(), any(), any(), any());
    }
}
