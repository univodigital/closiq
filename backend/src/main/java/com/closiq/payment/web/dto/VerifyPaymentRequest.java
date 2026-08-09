package com.closiq.payment.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class VerifyPaymentRequest {

    @NotBlank
    String paymentId;

    @NotBlank
    String razorpayOrderId;

    @NotBlank
    String razorpayPaymentId;

    @NotBlank
    String razorpaySignature;
}
