package com.closiq.seller.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class BulkProductImportRequest {

    @NotBlank
    String csvContent;
}
