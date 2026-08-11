package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DuplicateProductResponse {

    String productId;
    String slug;
    String productCode;
    String title;
    String status;
}
