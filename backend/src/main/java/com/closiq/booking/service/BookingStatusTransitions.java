package com.closiq.booking.service;

import com.closiq.booking.domain.BookingStatus;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;

import java.util.Map;
import java.util.Set;

/**
 * Central booking lifecycle transitions. Booking status remains separate from payment/refund status.
 */
public final class BookingStatusTransitions {

    /** Virtual pipeline step — not stored on {@code booking.status}. */
    public static final String SELLER_PREPARING = "SELLER_PREPARING";
    /** Virtual pipeline step for outbound delivery complete. */
    public static final String DELIVERED = "DELIVERED";

    private static final Map<String, Set<String>> ALLOWED = Map.ofEntries(
            Map.entry(BookingStatus.PENDING_PAYMENT, Set.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED)),
            Map.entry(BookingStatus.CONFIRMED, Set.of(
                    BookingStatus.SELLER_ACCEPTED, BookingStatus.CANCELLED, BookingStatus.REFUND_PENDING)),
            Map.entry(BookingStatus.SELLER_ACCEPTED, Set.of(
                    BookingStatus.PREPARING, BookingStatus.CANCELLED, BookingStatus.REFUND_PENDING)),
            Map.entry(BookingStatus.PREPARING, Set.of(
                    BookingStatus.OUT_FOR_DELIVERY, BookingStatus.CANCELLED, BookingStatus.REFUND_PENDING)),
            Map.entry(BookingStatus.OUT_FOR_DELIVERY, Set.of(
                    BookingStatus.TRIAL_READY, BookingStatus.CANCELLED, BookingStatus.REFUND_PENDING)),
            Map.entry(BookingStatus.TRIAL_READY, Set.of(
                    BookingStatus.RENTAL_ACTIVE, BookingStatus.TRIAL_REJECTED)),
            Map.entry(BookingStatus.TRIAL_REJECTED, Set.of(BookingStatus.RETURN_SCHEDULED)),
            Map.entry(BookingStatus.RENTAL_ACTIVE, Set.of(BookingStatus.RETURN_SCHEDULED)),
            Map.entry(BookingStatus.RETURN_SCHEDULED, Set.of(BookingStatus.RETURN_IN_TRANSIT)),
            Map.entry(BookingStatus.RETURN_IN_TRANSIT, Set.of(BookingStatus.RETURNED)),
            Map.entry(BookingStatus.RETURNED, Set.of(BookingStatus.INSPECTION)),
            Map.entry(BookingStatus.INSPECTION, Set.of(BookingStatus.DEPOSIT_REFUNDED)),
            Map.entry(BookingStatus.DEPOSIT_REFUNDED, Set.of(BookingStatus.COMPLETED)),
            Map.entry(BookingStatus.REFUND_PENDING, Set.of(BookingStatus.CANCELLED)),
            Map.entry(BookingStatus.CANCELLED, Set.of()),
            Map.entry(BookingStatus.COMPLETED, Set.of()));

    private BookingStatusTransitions() {
    }

    public static void assertTransition(String from, String to) {
        if (from.equals(to)) {
            return;
        }
        Set<String> targets = ALLOWED.get(from);
        if (targets == null || !targets.contains(to)) {
            throw new ClosiqException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    "Cannot transition booking from " + from + " to " + to);
        }
    }

    public static boolean canTransition(String from, String to) {
        if (from.equals(to)) {
            return true;
        }
        Set<String> targets = ALLOWED.get(from);
        return targets != null && targets.contains(to);
    }

    /** Lifecycle rank for timeline ordering (higher = further in pipeline). */
    public static int pipelineRank(String status) {
        return switch (status) {
            case BookingStatus.PENDING_PAYMENT -> 0;
            case BookingStatus.CONFIRMED -> 10;
            case BookingStatus.SELLER_ACCEPTED, BookingStatus.PREPARING, SELLER_PREPARING -> 20;
            case BookingStatus.OUT_FOR_DELIVERY -> 30;
            case DELIVERED -> 40;
            case BookingStatus.TRIAL_READY -> 50;
            case BookingStatus.RENTAL_ACTIVE -> 60;
            case BookingStatus.RETURN_SCHEDULED, BookingStatus.RETURN_IN_TRANSIT,
                 BookingStatus.RETURNED, BookingStatus.INSPECTION, BookingStatus.DEPOSIT_REFUNDED -> 70;
            case BookingStatus.COMPLETED -> 80;
            case BookingStatus.TRIAL_REJECTED -> 55;
            case BookingStatus.CANCELLED, BookingStatus.REFUND_PENDING -> -1;
            default -> 0;
        };
    }

    public static boolean isTerminal(String status) {
        return BookingStatus.COMPLETED.equals(status)
                || BookingStatus.CANCELLED.equals(status)
                || BookingStatus.DEPOSIT_REFUNDED.equals(status);
    }
}
