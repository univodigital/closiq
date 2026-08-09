package com.closiq.payment.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ValidateCouponResponse {

    boolean valid;
    long discountAmount;
    String message;
}
