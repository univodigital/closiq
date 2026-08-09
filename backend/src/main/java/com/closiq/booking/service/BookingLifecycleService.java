package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.domain.TrialSession;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.repository.TrialSessionRepository;
import com.closiq.booking.web.dto.ReturnRequestRequest;
import com.closiq.booking.web.dto.TrialRejectRequest;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.IdGenerator;
import com.closiq.notification.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingLifecycleService {

    private final BookingRepository bookingRepository;
    private final TrialSessionRepository trialSessionRepository;
    private final BookingTimelineService timelineService;
    private final NotificationDispatchService notificationDispatchService;

    @Transactional
    public Map<String, Object> acceptTrial(UUID customerId, String bookingIdOrNumber) {
        Booking booking = resolveBooking(customerId, bookingIdOrNumber);
        if (!BookingStatus.TRIAL_READY.equals(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        TrialSession trial = trialSessionRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION));

        Instant now = Instant.now();
        if (trial.getExpiresAt() != null && now.isAfter(trial.getExpiresAt())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Trial window has expired");
        }

        trial.setOutcome("ACCEPTED");
        trial.setAcceptedAt(now);
        trialSessionRepository.save(trial);

        booking.setStatus(BookingStatus.RENTAL_ACTIVE);
        bookingRepository.save(booking);
        timelineService.append(booking.getId(), customerId, BookingStatus.RENTAL_ACTIVE, "Trial accepted — rental active");

        return Map.of("status", BookingStatus.RENTAL_ACTIVE, "rentalActiveAt", now.toString());
    }

    @Transactional
    public Map<String, Object> rejectTrial(UUID customerId, String bookingIdOrNumber, TrialRejectRequest request) {
        Booking booking = resolveBooking(customerId, bookingIdOrNumber);
        if (!BookingStatus.TRIAL_READY.equals(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        TrialSession trial = trialSessionRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION));

        Instant now = Instant.now();
        trial.setOutcome("REJECTED");
        trial.setRejectedAt(now);
        trial.setRejectReason(request.getReason());
        trial.setRejectComment(request.getComment());
        trialSessionRepository.save(trial);

        booking.setStatus(BookingStatus.TRIAL_REJECTED);
        bookingRepository.save(booking);
        timelineService.append(booking.getId(), customerId, BookingStatus.TRIAL_REJECTED, "Trial rejected — return initiated");

        return Map.of(
                "status", BookingStatus.TRIAL_REJECTED,
                "refundAmount", booking.getRentalAmount(),
                "depositRefundAmount", booking.getDepositAmount());
    }

    @Transactional
    public Map<String, Object> requestReturn(UUID customerId, String bookingIdOrNumber, ReturnRequestRequest request) {
        Booking booking = resolveBooking(customerId, bookingIdOrNumber);
        if (!BookingStatus.RENTAL_ACTIVE.equals(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        if (request.getPickupDate().isBefore(booking.getRentalEndDate())) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Pickup date must be on or after rental end date");
        }

        booking.setStatus(BookingStatus.RETURN_SCHEDULED);
        bookingRepository.save(booking);
        timelineService.append(
                booking.getId(),
                customerId,
                BookingStatus.RETURN_SCHEDULED,
                "Return pickup scheduled for " + request.getPickupDate());

        notificationDispatchService.returnScheduled(booking);

        return Map.of("status", BookingStatus.RETURN_SCHEDULED, "pickupScheduledAt", Instant.now().toString());
    }

    @Transactional
    public TrialSession ensureTrialSession(UUID bookingId) {
        return trialSessionRepository.findByBookingId(bookingId).orElseGet(() -> {
            Instant started = Instant.now();
            TrialSession session = TrialSession.builder()
                    .id(IdGenerator.uuidV7())
                    .bookingId(bookingId)
                    .startedAt(started)
                    .expiresAt(started.plusSeconds(15 * 60L))
                    .outcome("PENDING")
                    .build();
            return trialSessionRepository.save(session);
        });
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
