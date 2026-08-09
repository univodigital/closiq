package com.closiq.admin.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class AdminReviewListItemResponse {

    String id;
    String authorDisplayName;
    String productTitle;
    short productRating;
    Short sellerRating;
    String title;
    String body;
    String status;
    Instant createdAt;
    Instant publishedAt;
}
