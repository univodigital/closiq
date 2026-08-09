package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;

@Value
@Builder
public class SellerBookingListItemResponse {

    String id;
    String rentalNumber;
    String orderNumber;
    /** @deprecated Use {@link #rentalNumber}. Kept for backward compatibility. */
    String bookingId;
    /** @deprecated Use {@link #orderNumber}. Kept for backward compatibility. */
    String orderId;
    String productId;
    String productTitle;
    String productImage;
    String customerName;
    String variantSize;
    String status;
    LocalDate rentalStart;
    LocalDate rentalEnd;
    int rentalDays;
    long earnings;
    long commission;
    String currency;
    String deliveryPincode;
    Instant prepBy;
    String notes;
}
