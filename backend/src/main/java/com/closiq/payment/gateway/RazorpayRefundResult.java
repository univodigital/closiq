package com.closiq.payment.gateway;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RazorpayRefundResult {

    String providerRefundId;
    long amountPaise;
    String status;
}
