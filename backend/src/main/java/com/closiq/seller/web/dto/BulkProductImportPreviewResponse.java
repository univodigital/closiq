package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BulkProductImportPreviewResponse {

    int totalRows;
    int validRows;
    int errorRows;
    List<RowPreview> rows;

    @Value
    @Builder
    public static class RowPreview {
        int rowNumber;
        boolean valid;
        String title;
        List<String> errors;
    }
}
