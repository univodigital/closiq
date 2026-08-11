package com.closiq.seller.service;

import com.closiq.catalog.domain.Category;
import com.closiq.catalog.repository.CategoryRepository;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.seller.web.dto.BulkProductImportPreviewResponse;
import com.closiq.seller.web.dto.BulkProductImportResultResponse;
import com.closiq.seller.web.dto.CreateSellerProductRequest;
import com.closiq.seller.web.dto.SellerProductResponse;
import com.closiq.user.domain.SellerProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerProductBulkImportService {

    private static final String ACTIVE_CATEGORY = "ACTIVE";

    private static final List<String> REQUIRED_HEADERS = List.of(
            "title",
            "description",
            "categoryslug",
            "audience",
            "garmenttype",
            "priceperday",
            "deposit",
            "city",
            "variants");

    private final SellerContextService sellerContextService;
    private final SellerProductService sellerProductService;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public BulkProductImportPreviewResponse previewImport(UUID userId, String csvContent) {
        sellerContextService.requireVerifiedSeller(userId);
        List<ParsedRow> rows = parseCsv(csvContent);
        List<BulkProductImportPreviewResponse.RowPreview> previews = new ArrayList<>();
        int valid = 0;

        for (ParsedRow row : rows) {
            List<String> errors = validateRow(row);
            boolean rowValid = errors.isEmpty();
            if (rowValid) {
                valid++;
            }
            previews.add(BulkProductImportPreviewResponse.RowPreview.builder()
                    .rowNumber(row.rowNumber())
                    .valid(rowValid)
                    .title(row.values().getOrDefault("title", ""))
                    .errors(errors)
                    .build());
        }

        return BulkProductImportPreviewResponse.builder()
                .totalRows(rows.size())
                .validRows(valid)
                .errorRows(rows.size() - valid)
                .rows(previews)
                .build();
    }

    @Transactional
    public BulkProductImportResultResponse importProducts(UUID userId, String csvContent) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        List<ParsedRow> rows = parseCsv(csvContent);
        List<BulkProductImportResultResponse.ImportRowResult> results = new ArrayList<>();
        int imported = 0;

        for (ParsedRow row : rows) {
            List<String> errors = validateRow(row);
            if (!errors.isEmpty()) {
                results.add(BulkProductImportResultResponse.ImportRowResult.builder()
                        .rowNumber(row.rowNumber())
                        .success(false)
                        .title(row.values().getOrDefault("title", ""))
                        .error(String.join("; ", errors))
                        .build());
                continue;
            }

            try {
                CreateSellerProductRequest request = toCreateRequest(row);
                SellerProductResponse created = sellerProductService.createProduct(
                        userId, "bulk-import-" + seller.getId() + "-" + row.rowNumber() + "-" + UUID.randomUUID(), request);
                imported++;
                results.add(BulkProductImportResultResponse.ImportRowResult.builder()
                        .rowNumber(row.rowNumber())
                        .success(true)
                        .productId(created.getId().toString())
                        .title(created.getTitle())
                        .build());
            } catch (ClosiqException ex) {
                results.add(BulkProductImportResultResponse.ImportRowResult.builder()
                        .rowNumber(row.rowNumber())
                        .success(false)
                        .title(row.values().getOrDefault("title", ""))
                        .error(ex.getMessage())
                        .build());
            }
        }

        return BulkProductImportResultResponse.builder()
                .totalRows(rows.size())
                .importedCount(imported)
                .failedCount(rows.size() - imported)
                .results(results)
                .build();
    }

    private CreateSellerProductRequest toCreateRequest(ParsedRow row) {
        Map<String, String> values = row.values();
        Category category = categoryRepository
                .findBySlugAndStatus(values.get("categoryslug"), ACTIVE_CATEGORY)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Category not found"));

        List<CreateSellerProductRequest.VariantInput> variants = parseVariants(values.get("variants"));
        return new CreateSellerProductRequest(
                values.get("title"),
                values.get("description"),
                category.getId(),
                category.getSlug(),
                values.get("audience").toLowerCase(Locale.ROOT),
                values.get("garmenttype"),
                blankToNull(values.get("designer")),
                Long.parseLong(values.get("priceperday")),
                Long.parseLong(values.get("deposit")),
                variants,
                values.get("city"));
    }

    private List<String> validateRow(ParsedRow row) {
        List<String> errors = new ArrayList<>();
        Map<String, String> values = row.values();

        if (isBlank(values.get("title")) || values.get("title").length() < 5) {
            errors.add("Title must be at least 5 characters");
        }
        if (isBlank(values.get("description")) || values.get("description").length() < 50) {
            errors.add("Description must be at least 50 characters");
        }
        if (isBlank(values.get("categoryslug"))
                || categoryRepository.findBySlugAndStatus(values.get("categoryslug"), ACTIVE_CATEGORY).isEmpty()) {
            errors.add("Invalid category");
        }
        if (isBlank(values.get("audience"))
                || !values.get("audience").matches("(?i)men|women|kids")) {
            errors.add("Audience must be men, women, or kids");
        }
        if (isBlank(values.get("garmenttype"))) {
            errors.add("Garment type is required");
        }
        if (!isPositiveLong(values.get("priceperday"))) {
            errors.add("Invalid rental price");
        }
        if (!isPositiveLong(values.get("deposit"))) {
            errors.add("Invalid deposit");
        } else if (isPositiveLong(values.get("priceperday"))
                && Long.parseLong(values.get("deposit")) < Long.parseLong(values.get("priceperday"))) {
            errors.add("Deposit must be at least the daily rental price");
        }
        if (isBlank(values.get("city"))) {
            errors.add("City is required");
        }
        try {
            if (parseVariants(values.get("variants")).isEmpty()) {
                errors.add("At least one variant is required (e.g. M:2|L:1)");
            }
        } catch (IllegalArgumentException ex) {
            errors.add(ex.getMessage());
        }
        return errors;
    }

    private List<CreateSellerProductRequest.VariantInput> parseVariants(String raw) {
        if (isBlank(raw)) {
            return List.of();
        }
        List<CreateSellerProductRequest.VariantInput> variants = new ArrayList<>();
        for (String part : raw.split("\\|")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] pieces = trimmed.split(":");
            if (pieces.length != 2) {
                throw new IllegalArgumentException("Variants must use SIZE:QTY format (e.g. M:2|L:1)");
            }
            int quantity = Integer.parseInt(pieces[1].trim());
            if (quantity < 1) {
                throw new IllegalArgumentException("Variant quantity must be at least 1");
            }
            variants.add(new CreateSellerProductRequest.VariantInput(pieces[0].trim(), quantity));
        }
        return variants;
    }

    private List<ParsedRow> parseCsv(String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "CSV content is required");
        }

        List<String[]> lines = CsvParser.parse(csvContent.trim());
        if (lines.isEmpty()) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "CSV file is empty");
        }

        String[] header = lines.getFirst();
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (int i = 0; i < header.length; i++) {
            headerIndex.put(header[i].trim().toLowerCase(Locale.ROOT), i);
        }
        for (String required : REQUIRED_HEADERS) {
            if (!headerIndex.containsKey(required)) {
                throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Missing required column: " + required);
            }
        }

        List<ParsedRow> rows = new ArrayList<>();
        for (int lineIdx = 1; lineIdx < lines.size(); lineIdx++) {
            String[] cells = lines.get(lineIdx);
            if (isRowBlank(cells)) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
                int idx = entry.getValue();
                values.put(entry.getKey(), idx < cells.length ? cells[idx].trim() : "");
            }
            rows.add(new ParsedRow(lineIdx + 1, values));
        }
        return rows;
    }

    private boolean isRowBlank(String[] cells) {
        for (String cell : cells) {
            if (cell != null && !cell.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private boolean isPositiveLong(String value) {
        if (isBlank(value)) {
            return false;
        }
        try {
            return Long.parseLong(value) >= 100;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private record ParsedRow(int rowNumber, Map<String, String> values) {
    }

    static final class CsvParser {

        private CsvParser() {
        }

        static List<String[]> parse(String content) {
            List<String[]> rows = new ArrayList<>();
            List<String> current = new ArrayList<>();
            StringBuilder cell = new StringBuilder();
            boolean inQuotes = false;

            for (int i = 0; i < content.length(); i++) {
                char ch = content.charAt(i);
                if (inQuotes) {
                    if (ch == '"') {
                        if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                            cell.append('"');
                            i++;
                        } else {
                            inQuotes = false;
                        }
                    } else {
                        cell.append(ch);
                    }
                } else if (ch == '"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    current.add(cell.toString());
                    cell.setLength(0);
                } else if (ch == '\n') {
                    current.add(cell.toString());
                    cell.setLength(0);
                    rows.add(current.toArray(String[]::new));
                    current = new ArrayList<>();
                } else if (ch != '\r') {
                    cell.append(ch);
                }
            }

            current.add(cell.toString());
            if (!current.isEmpty() && !(current.size() == 1 && current.getFirst().isBlank())) {
                rows.add(current.toArray(String[]::new));
            }
            return rows;
        }
    }
}
