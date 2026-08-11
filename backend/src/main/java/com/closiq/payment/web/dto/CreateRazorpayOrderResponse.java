package com.closiq.payment.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class CreateRazorpayOrderResponse {

    String paymentId;
    String razorpayOrderId;
    long amount;
    long amountInRupees;
    String currency;
    String keyId;
    String bookingId;
    String checkoutBatchId;
    int itemCount;
    Instant expiresAt;
}
