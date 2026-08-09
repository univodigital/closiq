package com.closiq.catalog.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CategoryResponse {

    String id;
    String slug;
    String name;
    String description;
    String image;
    long productCount;
    boolean featured;
    int sortOrder;
}
