package com.closiq.seller.service;

import com.closiq.seller.web.dto.SellerAnalyticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerAnalyticsService {

    private final SellerContextService sellerContextService;

    @Transactional(readOnly = true)
    public SellerAnalyticsResponse getAnalytics(UUID userId, String period) {
        sellerContextService.requireVerifiedSeller(userId);
        String resolvedPeriod = period != null && !period.isBlank() ? period : "30d";

        return SellerAnalyticsResponse.builder()
                .period(resolvedPeriod)
                .views(0)
                .uniqueVisitors(0)
                .bookings(0)
                .conversionRate(0.0)
                .revenue(0)
                .currency("INR")
                .topProducts(List.of())
                .build();
    }
}
