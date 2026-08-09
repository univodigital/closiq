package com.closiq.review.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class CreateReviewResponse {

    UUID reviewId;
    UUID productId;
    int productRating;
    Integer sellerRating;
    String comment;
    boolean verifiedRental;
    Instant createdAt;
}
