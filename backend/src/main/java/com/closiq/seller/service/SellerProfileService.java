package com.closiq.seller.service;

import com.closiq.catalog.repository.ProductRepository;
import com.closiq.seller.web.dto.SellerBusinessProfileResponse;
import com.closiq.user.domain.SellerProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private static final String ACTIVE_PRODUCT = "ACTIVE";

    private final SellerContextService sellerContextService;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public SellerBusinessProfileResponse getBusinessProfile(UUID userId) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        long listingCount = productRepository.countBySellerProfileIdAndStatusAndDeletedAtIsNull(
                seller.getId(), ACTIVE_PRODUCT);

        return SellerBusinessProfileResponse.builder()
                .sellerId(seller.getId().toString())
                .businessName(seller.getBusinessName())
                .verificationStatus(mapVerificationStatus(seller.getStatus()))
                .city(seller.getCity())
                .rating(seller.getAvgRating() != null ? seller.getAvgRating().doubleValue() : null)
                .listingCount(listingCount)
                .build();
    }

    private String mapVerificationStatus(String status) {
        if ("ACTIVE".equals(status)) {
            return "VERIFIED";
        }
        return status;
    }
}
