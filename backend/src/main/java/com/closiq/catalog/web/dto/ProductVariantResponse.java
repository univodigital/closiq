package com.closiq.catalog.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProductVariantResponse {

    String id;
    String size;
    boolean available;
}
