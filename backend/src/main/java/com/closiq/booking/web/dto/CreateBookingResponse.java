package com.closiq.booking.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;

@Value
@Builder
public class CreateBookingResponse {

    String bookingId;
    String rentalNumber;
    /** @deprecated Use {@link #rentalNumber}. Kept for backward compatibility. */
    String bookingNumber;
    String orderNumber;
    String status;
    Instant holdExpiresAt;
    String checkoutSessionId;
    ProductSnippet product;
    LocalDate rentalStartDate;
    LocalDate rentalEndDate;
    short rentalDays;
    boolean includesTrial;
    short trialDurationMinutes;
    PricingBreakdown pricing;

    @Value
    @Builder
    public static class ProductSnippet {
        String id;
        String title;
        String variantSize;
    }

    @Value
    @Builder
    public static class PricingBreakdown {
        long rentalAmount;
        long depositAmount;
        long deliveryFee;
        long discountAmount;
        long totalAmount;
        String currency;
    }
}
