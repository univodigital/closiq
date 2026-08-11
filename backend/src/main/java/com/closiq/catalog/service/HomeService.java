package com.closiq.catalog.service;

import com.closiq.catalog.web.dto.ProductSummaryResponse;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.user.service.UserPreferencesHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ProductCatalogService productCatalogService;
    private final UserProfileRepository userProfileRepository;
    private final UserPreferencesHelper preferencesHelper;

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> featuredProducts(int limit, LocalDate startDate, LocalDate endDate) {
        int size = Math.min(Math.max(limit, 1), 20);
        return productCatalogService.listProducts(
                null, null, null, null, null, null, true, null, null, null, null, null, size, startDate, endDate)
                .getItems();
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> trendingProducts(int limit, LocalDate startDate, LocalDate endDate) {
        int size = Math.min(Math.max(limit, 1), 20);
        return productCatalogService.listProducts(
                null, null, null, null, null, null, null, true, null, null, null, null, size, startDate, endDate)
                .getItems();
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> recommendations(UUID userId, int limit) {
        int size = Math.min(Math.max(limit, 1), 20);

        if (userId == null) {
            return trendingProducts(size, null, null);
        }

        return userProfileRepository.findByUserId(userId)
                .map(profile -> {
                    var shopping = preferencesHelper.getShopping(profile.getPreferences());
                    if (shopping.occasions() != null && !shopping.occasions().isEmpty()) {
                        String occasion = String.join(",", shopping.occasions());
                        return productCatalogService
                                .listProducts(occasion, null, shopping.size(), null, null, null, null, null, null, null, null, null, size, null, null)
                                .getItems();
                    }
                    return trendingProducts(size, null, null);
                })
                .orElseGet(() -> trendingProducts(size, null, null));
    }
}
