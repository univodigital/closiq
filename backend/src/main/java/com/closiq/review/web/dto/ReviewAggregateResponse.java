package com.closiq.review.web.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class ReviewAggregateResponse {

    BigDecimal averageRating;
    long totalCount;
}
