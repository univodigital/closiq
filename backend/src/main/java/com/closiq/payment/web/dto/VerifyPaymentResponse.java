package com.closiq.payment.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VerifyPaymentResponse {

    String paymentId;
    String status;
    String bookingId;
    String rentalNumber;
    /** @deprecated Use {@link #rentalNumber}. Kept for backward compatibility. */
    String bookingNumber;
    String orderNumber;
    String bookingStatus;
    long paidAmount;
    String currency;
}
