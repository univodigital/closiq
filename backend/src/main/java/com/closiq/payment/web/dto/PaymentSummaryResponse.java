package com.closiq.payment.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class PaymentSummaryResponse {

    String paymentId;
    String rentalNumber;
    /** @deprecated Use {@link #rentalNumber}. Kept for backward compatibility. */
    String bookingNumber;
    String orderNumber;
    long amount;
    long amountInRupees;
    String status;
    String method;
    Instant createdAt;
}
