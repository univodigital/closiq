package com.closiq.user.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProductSummaryResponse {

    String id;
    String slug;
    String title;
    long pricePerDay;
    long deposit;
    String currency;
    String imageUrl;
}
