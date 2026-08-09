package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class SellerBookingDetailResponse {

    String id;
    String rentalNumber;
    String orderNumber;
    /** @deprecated Use {@link #rentalNumber}. Kept for backward compatibility. */
    String bookingId;
    /** @deprecated Use {@link #orderNumber}. Kept for backward compatibility. */
    String orderId;
    String status;
    String productId;
    String productTitle;
    String productImage;
    String variantSize;
    LocalDate rentalStart;
    LocalDate rentalEnd;
    int rentalDays;
    String currency;
    EarningsBreakdown earnings;
    CustomerContact customer;
    Instant prepBy;
    String notes;
    String customerNotes;
    List<PrepChecklistItem> prepChecklist;

    @Value
    @Builder
    public static class EarningsBreakdown {
        long rentalAmount;
        long commission;
        long netEarnings;
        long depositHeld;
    }

    @Value
    @Builder
    public static class CustomerContact {
        String name;
        String phoneMasked;
        String deliveryPincode;
        String deliveryCity;
    }

    @Value
    @Builder
    public static class PrepChecklistItem {
        String item;
        boolean done;
    }
}
