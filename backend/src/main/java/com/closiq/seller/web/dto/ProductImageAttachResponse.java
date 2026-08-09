package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProductImageAttachResponse {

    String imageId;
    String url;
    short sortOrder;
    String alt;
}
