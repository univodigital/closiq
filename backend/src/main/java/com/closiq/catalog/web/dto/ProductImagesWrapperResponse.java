package com.closiq.catalog.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ProductImagesWrapperResponse {

    List<ProductImageResponse> images;
}
