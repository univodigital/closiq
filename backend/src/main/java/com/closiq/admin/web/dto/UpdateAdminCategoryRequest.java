package com.closiq.admin.web.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class UpdateAdminCategoryRequest {

    @Size(min = 2, max = 100)
    String name;

    @Size(max = 2000)
    String description;

    @Size(max = 512)
    String imageUrl;

    @Pattern(regexp = "ACTIVE|DEPRECATED")
    String status;

    Boolean featured;

    Short sortOrder;
}
