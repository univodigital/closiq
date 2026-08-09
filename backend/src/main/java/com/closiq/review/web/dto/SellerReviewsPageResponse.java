package com.closiq.review.web.dto;

import com.closiq.catalog.web.dto.ProductReviewResponse;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SellerReviewsPageResponse {

    List<ProductReviewResponse> reviews;
    ReviewAggregateResponse aggregate;
}
