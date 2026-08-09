package com.closiq.admin.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class AdminSellerApplicationListItemResponse {

    String applicationId;
    String userId;
    String applicantName;
    String applicantPhone;
    String businessName;
    String businessType;
    String city;
    String status;
    Instant submittedAt;
}
