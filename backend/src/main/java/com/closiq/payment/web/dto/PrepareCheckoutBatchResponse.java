package com.closiq.payment.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class PrepareCheckoutBatchResponse {

    String checkoutBatchId;
    long totalAmount;
    long discountAmount;
    String currency;
    Instant holdExpiresAt;
    List<BookingHold> bookings;

    @Value
    @Builder
    public static class BookingHold {
        String bookingId;
        String rentalNumber;
        String checkoutSessionId;
        long totalAmount;
    }
}
