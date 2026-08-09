package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class SellerProductListItemResponse {

    String id;
    String productCode;
    String slug;
    String title;
    String status;
    long pricePerDay;
    long deposit;
    String primaryImageUrl;
    Instant createdAt;
    Instant publishedAt;
}
