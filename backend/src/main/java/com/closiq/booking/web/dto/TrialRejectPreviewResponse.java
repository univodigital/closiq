package com.closiq.booking.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TrialRejectPreviewResponse {

    String policyCode;
    String policyLabel;
    long rentalPaid;
    long rentalRefundAmount;
    long deliveryFeeNonRefundable;
    long depositAmount;
    long depositRefundAmount;
    String depositRefundTiming;
    String refundMethod;
    int rentalRefundExpectedBusinessDays;
    int depositRefundExpectedBusinessDaysMin;
    int depositRefundExpectedBusinessDaysMax;
}
