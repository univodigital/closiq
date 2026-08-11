package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SellerRejectPreviewResponse {

    long refundAmount;
    int expectedBusinessDays;
    String refundMethod;
    String currency;
}
