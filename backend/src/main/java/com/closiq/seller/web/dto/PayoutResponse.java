package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PayoutResponse {

    String payoutId;
    String status;
    long amount;
}
