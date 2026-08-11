package com.closiq.admin.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class AdminCategoryResponse {

    String id;
    String slug;
    String name;
    String description;
    String imageUrl;
    String status;
    boolean featured;
    short sortOrder;
    long productCount;
    Instant createdAt;
    Instant updatedAt;
}
