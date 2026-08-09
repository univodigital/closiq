package com.closiq.catalog.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProductImageResponse {

    String id;
    String url;
    int sortOrder;
    String alt;
}
