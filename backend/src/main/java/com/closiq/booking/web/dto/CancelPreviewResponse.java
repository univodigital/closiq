package com.closiq.booking.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CancelPreviewResponse {

    boolean eligible;
    String policyCode;
    String policyLabel;
    long originalAmount;
    long refundAmount;
    long nonRefundableAmount;
    long rentalRefundAmount;
    long depositRefundAmount;
    long deliveryFeeNonRefundable;
    String nonRefundableReason;
    String refundMethod;
    int expectedRefundBusinessDays;
}
