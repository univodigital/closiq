package com.closiq.catalog.web;

import com.closiq.catalog.service.ProductCatalogService;
import com.closiq.catalog.service.ProductQueryService;
import com.closiq.catalog.web.dto.ProductAvailabilityResponse;
import com.closiq.catalog.web.dto.ProductDetailResponse;
import com.closiq.catalog.web.dto.ProductFiltersResponse;
import com.closiq.catalog.web.dto.ProductImagesWrapperResponse;
import com.closiq.catalog.web.dto.ProductReviewResponse;
import com.closiq.catalog.web.dto.ProductSummaryResponse;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.common.web.ResponseMeta;
import com.closiq.common.web.ClosiqRequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product listing, search, and detail")
public class ProductController {

    private final ProductCatalogService productCatalogService;
    private final ProductQueryService productQueryService;

    @GetMapping
    @Operation(summary = "Paginated product listing with filters")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> listProducts(
            @RequestParam(required = false) String occasion,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean trending,
            @RequestParam(required = false) String audience,
            @RequestParam(required = false) String garmentType,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {

        PagedResult<ProductSummaryResponse> page = productCatalogService.listProducts(
                occasion, categoryId, size, minPrice, maxPrice, city, featured, trending, audience, garmentType,
                sort, pageToken, limit, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.ok(
                page.getItems(),
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(page)));
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search across products")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> searchProducts(
            @RequestParam String q,
            @RequestParam(required = false) String occasion,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {

        ProductCatalogService.SearchResult result = productCatalogService.searchProducts(
                q, occasion, size, minPrice, maxPrice, sort, pageToken, limit, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.okWithSearch(
                result.page().getItems(),
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(result.page()),
                ResponseMeta.SearchMeta.builder()
                        .query(result.query())
                        .totalCount(result.totalCount())
                        .tookMs(result.tookMs())
                        .build()));
    }

    @GetMapping("/filters")
    @Operation(summary = "Available filter facets for PLP")
    public ResponseEntity<ApiResponse<ProductFiltersResponse>> filters(
            @RequestParam(required = false) String occasion,
            @RequestParam(required = false) String q,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                productCatalogService.getFilters(occasion, q),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/{slugOrId}")
    @Operation(summary = "Product detail by slug or UUID")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(
            @PathVariable String slugOrId,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                productCatalogService.getProductBySlugOrId(slugOrId),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/{slugOrId}/availability")
    @Operation(summary = "Availability calendar for a variant")
    public ResponseEntity<ApiResponse<ProductAvailabilityResponse>> availability(
            @PathVariable String slugOrId,
            @RequestParam UUID variantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                productQueryService.getAvailability(slugOrId, variantId, startDate, endDate),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/{slugOrId}/related")
    @Operation(summary = "Related products")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> related(
            @PathVariable String slugOrId,
            @RequestParam(defaultValue = "4") int limit,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                productCatalogService.getRelatedProducts(slugOrId, limit),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/{slugOrId}/reviews")
    @Operation(summary = "Paginated product reviews")
    public ResponseEntity<ApiResponse<List<ProductReviewResponse>>> reviews(
            @PathVariable String slugOrId,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false, defaultValue = "createdAt:desc") String sort,
            HttpServletRequest request) {

        PagedResult<ProductReviewResponse> page = productQueryService.listReviews(slugOrId, pageToken, limit, sort);
        return ResponseEntity.ok(ApiResponse.ok(
                page.getItems(),
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(page)));
    }

    @GetMapping("/{slugOrId}/images")
    @Operation(summary = "Ordered product image gallery")
    public ResponseEntity<ApiResponse<ProductImagesWrapperResponse>> images(
            @PathVariable String slugOrId,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                productQueryService.listImages(slugOrId),
                ClosiqRequestIdFilter.getRequestId(request)));
    }
}
