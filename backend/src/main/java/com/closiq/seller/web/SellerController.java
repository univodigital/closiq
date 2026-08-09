package com.closiq.seller.web;

import com.closiq.common.security.RequiresSeller;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.seller.service.SellerAnalyticsService;
import com.closiq.seller.service.SellerDashboardService;
import com.closiq.seller.service.SellerProfileService;
import com.closiq.seller.web.dto.SellerAnalyticsResponse;
import com.closiq.seller.web.dto.SellerBusinessProfileResponse;
import com.closiq.seller.web.dto.SellerDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
@Tag(name = "Seller", description = "Seller dashboard and analytics")
public class SellerController {

    private final SellerDashboardService sellerDashboardService;
    private final SellerAnalyticsService sellerAnalyticsService;
    private final SellerProfileService sellerProfileService;

    @GetMapping("/profile")
    @RequiresSeller
    @Operation(summary = "Seller business profile")
    public ResponseEntity<ApiResponse<SellerBusinessProfileResponse>> profile(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        SellerBusinessProfileResponse response = sellerProfileService.getBusinessProfile(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/dashboard")
    @RequiresSeller
    @Operation(summary = "Seller dashboard summary")
    public ResponseEntity<ApiResponse<SellerDashboardResponse>> dashboard(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        SellerDashboardResponse response = sellerDashboardService.getDashboard(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/analytics")
    @RequiresSeller
    @Operation(summary = "Seller analytics")
    public ResponseEntity<ApiResponse<SellerAnalyticsResponse>> analytics(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "30d") String period,
            HttpServletRequest request) {

        SellerAnalyticsResponse response = sellerAnalyticsService.getAnalytics(principal.userId(), period);
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }
}
