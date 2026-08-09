package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class KycDocumentSummaryResponse {

    String type;
    String status;
    Instant uploadedAt;
}
