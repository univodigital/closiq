package com.closiq.payment.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

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
    long rentalAmount;
    long depositAmount;
    long deliveryFee;
    long discountAmount;
    String paymentMethod;
    Instant paidAt;
    String checkoutBatchId;
}
