package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class SellerApplicationDetailResponse {

    String applicationId;
    String status;
    String businessName;
    Instant submittedAt;
    Instant reviewedAt;
    String rejectionReason;
    List<KycDocumentSummaryResponse> documents;
}
