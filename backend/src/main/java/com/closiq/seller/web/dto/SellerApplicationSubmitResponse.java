package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class SellerApplicationSubmitResponse {

    String applicationId;
    String status;
    Instant submittedAt;
}
