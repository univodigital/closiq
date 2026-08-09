package com.closiq.payment.web.dto;

import lombok.Value;

@Value
public class ValidateCouponRequest {

    String couponCode;
    String bookingId;
}
