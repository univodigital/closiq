package com.closiq.payment.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CheckoutCalculateResponse {

    short rentalDays;
    List<LineItem> lineItems;
    long subtotal;
    long discountAmount;
    long totalAmount;
    long depositAmount;
    long payNowAmount;
    String currency;
    boolean serviceable;

    @Value
    @Builder
    public static class LineItem {
        String type;
        String label;
        long amount;
    }
}
