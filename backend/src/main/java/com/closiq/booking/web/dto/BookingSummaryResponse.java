package com.closiq.booking.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;

@Value
@Builder
public class BookingSummaryResponse {

    String id;
    String rentalNumber;
    /** @deprecated Use {@link #rentalNumber}. Kept for backward compatibility. */
    String bookingNumber;
    String orderNumber;
    String status;
    String productTitle;
    String productImage;
    String variantSize;
    LocalDate rentalStartDate;
    LocalDate rentalEndDate;
    long totalAmount;
    String currency;
    Instant createdAt;
}
