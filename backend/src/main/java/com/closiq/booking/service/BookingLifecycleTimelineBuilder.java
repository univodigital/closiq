package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.domain.BookingTimeline;
import com.closiq.booking.web.dto.TimelineEventResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the full order lifecycle timeline from backend state — not a frontend-only array.
 */
@Component
public class BookingLifecycleTimelineBuilder {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private record Step(String status, String label, String description) {}

    public List<TimelineEventResponse> build(Booking booking, List<BookingTimeline> history) {
        if (BookingStatus.CANCELLED.equals(booking.getStatus())
                || BookingStatus.REFUND_PENDING.equals(booking.getStatus())) {
            return buildCancelledTimeline(booking, history);
        }

        if (BookingStatus.TRIAL_REJECTED.equals(booking.getStatus())
                || history.stream().anyMatch(e -> BookingStatus.TRIAL_REJECTED.equals(e.getStatus()))) {
            return buildTrialRejectedTimeline(booking, history);
        }

        List<Step> pipeline = normalPipeline(booking.isIncludesTrial());
        Map<String, Instant> timestamps = indexTimestamps(history);
        String currentStatus = normalizeCurrentStatus(booking.getStatus());
        int currentRank = BookingStatusTransitions.pipelineRank(currentStatus);

        List<TimelineEventResponse> events = new ArrayList<>();
        boolean terminal = BookingStatus.COMPLETED.equals(booking.getStatus())
                || BookingStatus.DEPOSIT_REFUNDED.equals(booking.getStatus());

        for (Step step : pipeline) {
            int stepRank = BookingStatusTransitions.pipelineRank(step.status());
            Instant ts = resolveTimestamp(step.status(), timestamps, booking);
            boolean completed = terminal ? stepRank <= currentRank : stepRank < currentRank;
            boolean current = !terminal && step.status().equals(currentStatus);
            boolean pending = !terminal && stepRank > currentRank;

            String description = step.description();
            if (BookingStatusTransitions.DELIVERED.equals(step.status()) && ts != null) {
                description = "Delivered on " + formatInstantDate(ts);
            } else if (BookingStatus.RENTAL_ACTIVE.equals(step.status())) {
                description = "Rental active until " + formatLocalDate(booking.getRentalEndDate());
            }

            events.add(TimelineEventResponse.builder()
                    .status(step.status())
                    .label(step.label())
                    .description(description)
                    .timestamp(ts)
                    .completed(completed)
                    .current(current)
                    .pending(pending)
                    .build());
        }
        return events;
    }

    private List<TimelineEventResponse> buildTrialRejectedTimeline(
            Booking booking, List<BookingTimeline> history) {

        Map<String, Instant> timestamps = indexTimestamps(history);
        String currentStatus = booking.getStatus();
        int currentRank = rejectedPipelineRank(currentStatus);

        List<Step> pipeline = List.of(
                new Step(BookingStatus.TRIAL_READY, "Trial", "Home trial at your doorstep"),
                new Step(BookingStatus.TRIAL_REJECTED, "Rejected", "Outfit rejected during trial"),
                new Step(BookingStatus.RETURN_SCHEDULED, "Return pickup scheduled", "Agent will collect the outfit"),
                new Step(BookingStatus.RETURNED, "Returned", "Outfit received"),
                new Step(BookingStatus.INSPECTION, "Inspection", "Deposit inspection in progress"),
                new Step(BookingStatus.COMPLETED, "Completed", "Refund and deposit processing complete"));

        boolean terminal = BookingStatus.COMPLETED.equals(currentStatus)
                || BookingStatus.DEPOSIT_REFUNDED.equals(currentStatus);

        List<TimelineEventResponse> events = new ArrayList<>();
        for (Step step : pipeline) {
            int stepRank = rejectedPipelineRank(step.status());
            Instant ts = timestamps.get(step.status());
            boolean completed = terminal ? stepRank <= currentRank : stepRank < currentRank;
            boolean current = !terminal && step.status().equals(currentStatus)
                    || (!terminal && currentStatus.equals(BookingStatus.RETURN_IN_TRANSIT)
                            && BookingStatus.RETURN_SCHEDULED.equals(step.status()));
            boolean pending = !terminal && stepRank > currentRank;

            events.add(TimelineEventResponse.builder()
                    .status(step.status())
                    .label(step.label())
                    .description(step.description())
                    .timestamp(ts)
                    .completed(completed)
                    .current(current)
                    .pending(pending)
                    .build());
        }
        return events;
    }

    private int rejectedPipelineRank(String status) {
        return switch (status) {
            case BookingStatus.TRIAL_READY -> 10;
            case BookingStatus.TRIAL_REJECTED -> 20;
            case BookingStatus.RETURN_SCHEDULED, BookingStatus.RETURN_IN_TRANSIT -> 30;
            case BookingStatus.RETURNED -> 40;
            case BookingStatus.INSPECTION, BookingStatus.REFUND_PENDING -> 50;
            case BookingStatus.DEPOSIT_REFUNDED, BookingStatus.COMPLETED -> 60;
            default -> 0;
        };
    }

    private List<Step> normalPipeline(boolean includesTrial) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(BookingStatus.CONFIRMED, "Confirmed", "Payment received"));
        steps.add(new Step(
                BookingStatusTransitions.SELLER_PREPARING,
                "Seller preparing",
                "Seller is preparing your order"));
        steps.add(new Step(
                BookingStatus.OUT_FOR_DELIVERY,
                "Out for delivery",
                "Your order is on the way"));
        steps.add(new Step(
                BookingStatusTransitions.DELIVERED,
                "Delivered",
                "Item delivered to you"));
        if (includesTrial) {
            steps.add(new Step(BookingStatus.TRIAL_READY, "Trial", "Trial period started"));
        }
        steps.add(new Step(
                BookingStatus.RENTAL_ACTIVE,
                "Active rental",
                "Rental period in progress"));
        steps.add(new Step(
                BookingStatus.RETURN_SCHEDULED,
                "Return scheduled",
                "Return pickup scheduled"));
        steps.add(new Step(BookingStatus.COMPLETED, "Completed", "Rental lifecycle complete"));
        return steps;
    }

    private List<TimelineEventResponse> buildCancelledTimeline(Booking booking, List<BookingTimeline> history) {
        List<TimelineEventResponse> events = new ArrayList<>();
        for (BookingTimeline entry : history) {
            if (BookingStatus.CANCELLED.equals(entry.getStatus())
                    || BookingStatus.REFUND_PENDING.equals(entry.getStatus())) {
                continue;
            }
            events.add(TimelineEventResponse.builder()
                    .status(entry.getStatus())
                    .label(entry.getLabel())
                    .description(entry.getDescription())
                    .timestamp(entry.getOccurredAt())
                    .completed(true)
                    .current(false)
                    .pending(false)
                    .build());
        }
        events.add(TimelineEventResponse.builder()
                .status(booking.getStatus())
                .label("Cancelled")
                .description(booking.getCancelReason())
                .timestamp(booking.getCancelledAt())
                .completed(true)
                .current(true)
                .pending(false)
                .build());
        return events;
    }

    private String normalizeCurrentStatus(String status) {
        return switch (status) {
            case BookingStatus.SELLER_ACCEPTED, BookingStatus.PREPARING ->
                    BookingStatusTransitions.SELLER_PREPARING;
            case BookingStatus.TRIAL_READY -> BookingStatus.TRIAL_READY;
            case BookingStatus.RETURN_IN_TRANSIT, BookingStatus.RETURNED,
                 BookingStatus.INSPECTION, BookingStatus.DEPOSIT_REFUNDED ->
                    BookingStatus.RETURN_SCHEDULED;
            case BookingStatus.COMPLETED -> BookingStatus.COMPLETED;
            default -> status;
        };
    }

    private Map<String, Instant> indexTimestamps(List<BookingTimeline> history) {
        Map<String, Instant> map = new HashMap<>();
        for (BookingTimeline entry : history) {
            map.putIfAbsent(entry.getStatus(), entry.getOccurredAt());
            if (BookingStatus.SELLER_ACCEPTED.equals(entry.getStatus())
                    || BookingStatus.PREPARING.equals(entry.getStatus())) {
                map.putIfAbsent(BookingStatusTransitions.SELLER_PREPARING, entry.getOccurredAt());
            }
            if (BookingStatus.TRIAL_READY.equals(entry.getStatus())) {
                map.putIfAbsent(BookingStatusTransitions.DELIVERED, entry.getOccurredAt());
            }
            if (BookingStatus.DEPOSIT_REFUNDED.equals(entry.getStatus())
                    || BookingStatus.COMPLETED.equals(entry.getStatus())) {
                map.putIfAbsent(BookingStatus.COMPLETED, entry.getOccurredAt());
            }
        }
        return map;
    }

    private Instant resolveTimestamp(String stepStatus, Map<String, Instant> timestamps, Booking booking) {
        Instant ts = timestamps.get(stepStatus);
        if (ts != null) {
            return ts;
        }
        if (BookingStatus.CONFIRMED.equals(stepStatus)) {
            return booking.getConfirmedAt();
        }
        if (BookingStatus.COMPLETED.equals(stepStatus)) {
            return booking.getCompletedAt();
        }
        return null;
    }

    private String formatInstantDate(Instant instant) {
        return DATE_FMT.format(instant.atZone(DISPLAY_ZONE));
    }

    private String formatLocalDate(LocalDate date) {
        return DATE_FMT.format(date);
    }
}
