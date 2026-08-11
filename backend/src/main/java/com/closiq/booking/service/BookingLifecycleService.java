package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.domain.TrialSession;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.repository.TrialSessionRepository;
import com.closiq.booking.web.dto.ReturnRequestRequest;
import com.closiq.booking.web.dto.ReturnScheduleResponse;
import com.closiq.booking.web.dto.TrialRejectPreviewResponse;
import com.closiq.booking.web.dto.TrialRejectRequest;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.IdGenerator;
import com.closiq.notification.service.NotificationDispatchService;
import com.closiq.payment.service.RefundService;
import com.closiq.shipment.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingLifecycleService {

    private static final String OUTCOME_PENDING = "PENDING";
    private static final String OUTCOME_ACCEPTED = "ACCEPTED";
    private static final String OUTCOME_REJECTED = "REJECTED";
    private static final String OUTCOME_EXPIRED = "EXPIRED";

    private final BookingRepository bookingRepository;
    private final TrialSessionRepository trialSessionRepository;
    private final BookingTimelineService timelineService;
    private final NotificationDispatchService notificationDispatchService;
    private final RefundService refundService;
    private final TrialPolicyService trialPolicyService;
    private final ShipmentService shipmentService;

    @Transactional(readOnly = true)
    public TrialRejectPreviewResponse previewTrialReject(UUID customerId, String bookingIdOrNumber) {
        Booking booking = resolveBooking(customerId, bookingIdOrNumber);
        if (!trialPolicyService.isTrialDecisionAllowed(booking)) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Trial rejection preview not available");
        }
        return trialPolicyService.previewReject(booking);
    }

    @Transactional
    public Map<String, Object> acceptTrial(UUID customerId, String bookingIdOrNumber) {
        Booking booking = resolveBooking(customerId, bookingIdOrNumber);
        if (!BookingStatus.TRIAL_READY.equals(booking.getStatus())) {
            if (BookingStatus.RENTAL_ACTIVE.equals(booking.getStatus())) {
                TrialSession existing = trialSessionRepository.findByBookingId(booking.getId()).orElse(null);
                if (existing != null && OUTCOME_ACCEPTED.equals(existing.getOutcome())) {
                    return Map.of(
                            "status", BookingStatus.RENTAL_ACTIVE,
                            "rentalActiveAt", existing.getAcceptedAt() != null
                                    ? existing.getAcceptedAt().toString()
                                    : Instant.now().toString());
                }
            }
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        TrialSession trial = trialSessionRepository.findByBookingIdForUpdate(booking.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Trial session not found"));

        if (OUTCOME_ACCEPTED.equals(trial.getOutcome())) {
            return Map.of(
                    "status", BookingStatus.RENTAL_ACTIVE,
                    "rentalActiveAt", trial.getAcceptedAt().toString());
        }
        if (!OUTCOME_PENDING.equals(trial.getOutcome())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Your trial has already been completed.");
        }

        Instant now = Instant.now();
        assertTrialNotExpired(trial, now);

        trial.setOutcome(OUTCOME_ACCEPTED);
        trial.setAcceptedAt(now);
        trialSessionRepository.save(trial);

        BookingStatusTransitions.assertTransition(booking.getStatus(), BookingStatus.RENTAL_ACTIVE);
        booking.setStatus(BookingStatus.RENTAL_ACTIVE);
        bookingRepository.save(booking);
        timelineService.append(booking.getId(), customerId, BookingStatus.RENTAL_ACTIVE, "Trial accepted — rental active");
        notificationDispatchService.trialAccepted(booking);

        return Map.of("status", BookingStatus.RENTAL_ACTIVE, "rentalActiveAt", now.toString());
    }

    @Transactional
    public Map<String, Object> rejectTrial(UUID customerId, String bookingIdOrNumber, TrialRejectRequest request) {
        Booking booking = resolveBooking(customerId, bookingIdOrNumber);
        if (BookingStatus.TRIAL_REJECTED.equals(booking.getStatus())) {
            TrialRejectPreviewResponse preview = trialPolicyService.previewReject(booking);
            return Map.of(
                    "status", BookingStatus.TRIAL_REJECTED,
                    "refundAmount", preview.getRentalRefundAmount(),
                    "depositRefundAmount", preview.getDepositRefundAmount());
        }
        if (!BookingStatus.TRIAL_READY.equals(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        TrialSession trial = trialSessionRepository.findByBookingIdForUpdate(booking.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Trial session not found"));

        if (OUTCOME_REJECTED.equals(trial.getOutcome())) {
            TrialRejectPreviewResponse preview = trialPolicyService.previewReject(booking);
            return Map.of(
                    "status", BookingStatus.TRIAL_REJECTED,
                    "refundAmount", preview.getRentalRefundAmount(),
                    "depositRefundAmount", preview.getDepositRefundAmount());
        }
        if (!OUTCOME_PENDING.equals(trial.getOutcome())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Your trial has already been completed.");
        }

        Instant now = Instant.now();
        assertTrialNotExpired(trial, now);

        TrialRejectPreviewResponse preview = trialPolicyService.previewReject(booking);

        trial.setOutcome(OUTCOME_REJECTED);
        trial.setRejectedAt(now);
        trial.setRejectReason(request.getReason());
        trial.setRejectComment(request.getComment());
        trialSessionRepository.save(trial);

        BookingStatusTransitions.assertTransition(booking.getStatus(), BookingStatus.TRIAL_REJECTED);
        booking.setStatus(BookingStatus.TRIAL_REJECTED);
        bookingRepository.save(booking);
        timelineService.append(booking.getId(), customerId, BookingStatus.TRIAL_REJECTED, "Trial rejected — return initiated");

        initiateTrialRentalRefund(booking, customerId, preview, request);

        try {
            shipmentService.scheduleReturnPickup(customerId, bookingIdOrNumber, new ReturnRequestRequest(null));
        } catch (Exception ex) {
            log.warn("Return pickup scheduling deferred for trial reject booking {}: {}", booking.getId(), ex.getMessage());
        }

        notificationDispatchService.trialRejected(booking);

        return Map.of(
                "status", BookingStatus.TRIAL_REJECTED,
                "refundAmount", preview.getRentalRefundAmount(),
                "depositRefundAmount", preview.getDepositRefundAmount());
    }

    @Transactional
    public ReturnScheduleResponse requestReturn(UUID customerId, String bookingIdOrNumber, ReturnRequestRequest request) {
        Booking booking = resolveBooking(customerId, bookingIdOrNumber);
        if (!BookingStatus.RENTAL_ACTIVE.equals(booking.getStatus())
                && !BookingStatus.RETURN_SCHEDULED.equals(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Return is not available at this stage");
        }
        ReturnRequestRequest safeRequest = request != null ? request : new ReturnRequestRequest(null);
        return shipmentService.scheduleReturnPickup(customerId, bookingIdOrNumber, safeRequest);
    }

    @Transactional
    public TrialSession ensureTrialSession(UUID bookingId) {
        return trialSessionRepository.findByBookingId(bookingId).orElseGet(() -> {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
            int minutes = booking.getTrialDurationMinutes() > 0 ? booking.getTrialDurationMinutes() : 15;
            Instant started = Instant.now();
            TrialSession session = TrialSession.builder()
                    .id(IdGenerator.uuidV7())
                    .bookingId(bookingId)
                    .startedAt(started)
                    .expiresAt(started.plusSeconds(minutes * 60L))
                    .outcome(OUTCOME_PENDING)
                    .build();
            return trialSessionRepository.save(session);
        });
    }

    @Transactional
    public void expireTrialIfNeeded(TrialSession trial) {
        if (!OUTCOME_PENDING.equals(trial.getOutcome())) {
            return;
        }
        if (trial.getExpiresAt() != null && Instant.now().isAfter(trial.getExpiresAt())) {
            trial.setOutcome(OUTCOME_EXPIRED);
            trialSessionRepository.save(trial);
        }
    }

    private void initiateTrialRentalRefund(
            Booking booking, UUID customerId, TrialRejectPreviewResponse preview, TrialRejectRequest request) {

        if (preview.getRentalRefundAmount() <= 0) {
            return;
        }

        try {
            var payment = refundService.resolvePaymentForBooking(booking);
            refundService.initiateRefund(
                    payment.getId(),
                    booking.getId(),
                    RefundService.TYPE_RENTAL,
                    preview.getRentalRefundAmount() * 100L,
                    "trial-reject-rental-" + booking.getId(),
                    customerId,
                    request.getReason() != null ? request.getReason() : "Trial rejected",
                    preview.getRentalRefundExpectedBusinessDays());
        } catch (Exception ex) {
            log.warn("Trial reject rental refund deferred for booking {}: {}", booking.getId(), ex.getMessage());
        }
    }

    private void assertTrialNotExpired(TrialSession trial, Instant now) {
        if (trial.getExpiresAt() != null && now.isAfter(trial.getExpiresAt())) {
            if (OUTCOME_PENDING.equals(trial.getOutcome())) {
                trial.setOutcome(OUTCOME_EXPIRED);
                trialSessionRepository.save(trial);
            }
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Trial window has expired");
        }
    }

    private Booking resolveBooking(UUID customerId, String bookingIdOrNumber) {
        if (bookingIdOrNumber.startsWith("VST-RNT-") || bookingIdOrNumber.startsWith("BK-")) {
            return bookingRepository.findByRentalNumberAndCustomerId(bookingIdOrNumber, customerId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        }
        if (bookingIdOrNumber.startsWith("VST-ORD-")) {
            return bookingRepository.findByOrderNumberAndCustomerId(bookingIdOrNumber, customerId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        }
        try {
            UUID id = UUID.fromString(bookingIdOrNumber);
            return bookingRepository.findByIdAndCustomerId(id, customerId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        } catch (IllegalArgumentException ex) {
            throw new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found");
        }
    }
}
