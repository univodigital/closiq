package com.closiq.seller.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.config.ClosiqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class SellerAcceptanceService {

    public static final String EXPIRED_CANCEL_REASON = "ACCEPTANCE_EXPIRED";

    private final ClosiqProperties properties;

    public int acceptSlaHours() {
        return properties.getBooking().getSellerAcceptSlaHours();
    }

    public Instant acceptDeadline(Booking booking) {
        Instant base = booking.getConfirmedAt() != null ? booking.getConfirmedAt() : booking.getCreatedAt();
        return base.plus(acceptSlaHours(), ChronoUnit.HOURS);
    }

    public boolean isPendingAcceptance(Booking booking) {
        return BookingStatus.CONFIRMED.equals(booking.getStatus());
    }

    public boolean isAcceptanceExpired(Booking booking) {
        if (!isPendingAcceptance(booking)) {
            return false;
        }
        return Instant.now().isAfter(acceptDeadline(booking));
    }

    public void assertAcceptanceOpen(Booking booking) {
        if (!isPendingAcceptance(booking)) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        if (isAcceptanceExpired(booking)) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Acceptance window has expired");
        }
    }
}
