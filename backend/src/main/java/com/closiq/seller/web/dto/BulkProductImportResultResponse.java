package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BulkProductImportResultResponse {

    int totalRows;
    int importedCount;
    int failedCount;
    List<ImportRowResult> results;

    @Value
    @Builder
    public static class ImportRowResult {
        int rowNumber;
        boolean success;
        String productId;
        String title;
        String error;
    }
}
