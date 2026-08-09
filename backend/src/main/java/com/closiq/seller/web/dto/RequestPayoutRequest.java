package com.closiq.seller.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class RequestPayoutRequest {

    @NotNull
    @Min(value = 500, message = "Minimum payout amount is 500 INR")
    Long amount;

    @NotBlank
    String payoutMethodId;
}
