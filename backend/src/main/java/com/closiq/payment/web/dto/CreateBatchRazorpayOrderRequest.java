package com.closiq.payment.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.util.UUID;

@Value
public class CreateBatchRazorpayOrderRequest {

    @NotNull
    UUID checkoutBatchId;
}
