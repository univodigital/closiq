package com.closiq.review.web.dto;

import com.closiq.catalog.web.dto.ProductReviewResponse;
import com.closiq.common.web.PagedResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SellerReviewsResult {

    PagedResult<ProductReviewResponse> page;
    ReviewAggregateResponse aggregate;
}
