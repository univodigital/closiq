package com.closiq.catalog.web;

import com.closiq.catalog.service.HomeService;
import com.closiq.catalog.web.dto.ProductSummaryResponse;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
@Tag(name = "Home", description = "Featured, trending, and personalized discovery")
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/featured-products")
    @Operation(summary = "Curated featured products for home")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> featuredProducts(
            @RequestParam(defaultValue = "8") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                homeService.featuredProducts(limit, startDate, endDate),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/trending-products")
    @Operation(summary = "Trending products (7-day window proxy)")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> trendingProducts(
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                homeService.trendingProducts(limit, startDate, endDate),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Personalized recommendations (generic for guests)")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> recommendations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "8") int limit,
            HttpServletRequest request) {

        UUID userId = principal != null ? principal.userId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                homeService.recommendations(userId, limit),
                ClosiqRequestIdFilter.getRequestId(request)));
    }
}
