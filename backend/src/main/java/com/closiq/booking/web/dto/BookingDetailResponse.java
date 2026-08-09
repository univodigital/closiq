package com.closiq.booking.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class BookingDetailResponse {

    String id;
    String rentalNumber;
    /** @deprecated Use {@link #rentalNumber}. Kept for backward compatibility. */
    String bookingNumber;
    String orderNumber;
    String status;
    String productId;
    String productTitle;
    String productImage;
    String variantSize;
    LocalDate rentalStartDate;
    LocalDate rentalEndDate;
    short rentalDays;
    long rentalAmount;
    long depositAmount;
    long deliveryFee;
    long discountAmount;
    long totalAmount;
    String currency;
    boolean includesTrial;
    short trialDurationMinutes;
    Instant holdExpiresAt;
    Instant createdAt;
    DeliveryAddress deliveryAddress;
    RefundDetails refundDetails;
    List<TimelineEventResponse> timeline;

    @Value
    @Builder
    public static class DeliveryAddress {
        String line1;
        String line2;
        String city;
        String state;
        String pincode;
    }

    @Value
    @Builder
    public static class RefundDetails {
        long refundAmount;
        long depositRefundAmount;
        String status;
    }
}
