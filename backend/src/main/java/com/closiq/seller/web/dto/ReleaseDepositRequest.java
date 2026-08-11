package com.closiq.seller.web.dto;

import lombok.Value;

@Value
public class ReleaseDepositRequest {

    Long damageDeduction;
    Long lateFee;
    Long cleaningFee;
    String notes;
}
