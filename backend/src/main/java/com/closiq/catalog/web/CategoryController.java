package com.closiq.catalog.web;

import com.closiq.catalog.service.CategoryService;
import com.closiq.catalog.web.dto.CategoryResponse;
import com.closiq.catalog.web.dto.ProductSummaryResponse;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.common.web.ClosiqRequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Occasion/category navigation")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List occasion categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listCategories(
            @RequestParam(required = false) Boolean featured,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                categoryService.listCategories(featured),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/{slug}/products")
    @Operation(summary = "Products within a category")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> categoryProducts(
            @PathVariable String slug,
            @RequestParam(required = false) String occasion,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean trending,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {

        PagedResult<ProductSummaryResponse> page = categoryService.listCategoryProducts(
                slug, occasion, size, minPrice, maxPrice, city, featured, trending, sort, pageToken, limit);

        return ResponseEntity.ok(ApiResponse.ok(
                page.getItems(),
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(page)));
    }
}
