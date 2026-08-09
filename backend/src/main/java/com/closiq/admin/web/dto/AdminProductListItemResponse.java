package com.closiq.admin.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class AdminProductListItemResponse {

    String id;
    String productCode;
    String slug;
    String title;
    String status;
    String sellerBusinessName;
    String primaryImageUrl;
    long pricePerDay;
    Instant createdAt;
    Instant publishedAt;
}
