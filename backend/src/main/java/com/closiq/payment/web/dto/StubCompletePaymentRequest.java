package com.closiq.payment.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class StubCompletePaymentRequest {

    @NotBlank
    String paymentId;
}
