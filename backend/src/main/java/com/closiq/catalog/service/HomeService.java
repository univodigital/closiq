package com.closiq.catalog.service;

import com.closiq.catalog.web.dto.ProductSummaryResponse;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.user.service.UserPreferencesHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ProductCatalogService productCatalogService;
    private final UserProfileRepository userProfileRepository;
    private final UserPreferencesHelper preferencesHelper;

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> featuredProducts(int limit) {
        int size = Math.min(Math.max(limit, 1), 20);
        return productCatalogService.listProducts(null, null, null, null, null, null, true, null, null, null, null, null, size)
                .getItems();
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> trendingProducts(int limit) {
        int size = Math.min(Math.max(limit, 1), 20);
        return productCatalogService.listProducts(null, null, null, null, null, null, null, true, null, null, null, null, size)
                .getItems();
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> recommendations(UUID userId, int limit) {
        int size = Math.min(Math.max(limit, 1), 20);

        if (userId == null) {
            return trendingProducts(size);
        }

        return userProfileRepository.findByUserId(userId)
                .map(profile -> {
                    var shopping = preferencesHelper.getShopping(profile.getPreferences());
                    if (shopping.occasions() != null && !shopping.occasions().isEmpty()) {
                        String occasion = String.join(",", shopping.occasions());
                        return productCatalogService
                                .listProducts(occasion, null, shopping.size(), null, null, null, null, null, null, null, null, null, size)
                                .getItems();
                    }
                    return trendingProducts(size);
                })
                .orElseGet(() -> trendingProducts(size));
    }
}
