package com.closiq.user.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class WishlistItemResponse {

    String productId;
    Instant addedAt;
    ProductSummaryResponse product;
}
