package com.closiq.payment.gateway;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RazorpayOrderResult {

    String providerOrderId;
    long amountPaise;
    String currency;
}
