package com.closiq.admin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class CreateAdminCategoryRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    String name;

    @Size(max = 2000)
    String description;

    @Size(max = 512)
    String imageUrl;

    Boolean featured;

    Short sortOrder;
}
