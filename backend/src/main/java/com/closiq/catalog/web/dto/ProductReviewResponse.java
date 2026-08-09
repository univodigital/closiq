package com.closiq.catalog.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class ProductReviewResponse {

    String id;
    int rating;
    String comment;
    String customerDisplayName;
    List<String> photos;
    Instant createdAt;
    boolean verifiedRental;
}
