package com.closiq.review.service;

import com.closiq.catalog.domain.Product;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.review.domain.ReviewStatus;
import com.closiq.review.repository.ReviewRepository;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewAggregateService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final SellerProfileRepository sellerProfileRepository;

    @Transactional
    public void refreshProductAggregate(UUID productId) {
        Product product = productRepository.findById(productId).orElseThrow();
        long count = reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.PUBLISHED);
        BigDecimal avg = reviewRepository.averageProductRating(productId);
        product.setReviewCount((int) count);
        product.setAvgRating(count > 0 ? avg.setScale(1, RoundingMode.HALF_UP) : null);
        productRepository.save(product);
    }

    @Transactional
    public void refreshSellerAggregate(UUID sellerProfileId) {
        SellerProfile seller = sellerProfileRepository.findById(sellerProfileId).orElseThrow();
        long count = reviewRepository.countBySellerProfileIdAndStatus(sellerProfileId, ReviewStatus.PUBLISHED);
        if (count == 0) {
            seller.setAvgRating(null);
        } else {
            BigDecimal avg = reviewRepository.averageSellerRating(sellerProfileId);
            seller.setAvgRating(avg.setScale(1, RoundingMode.HALF_UP));
        }
        sellerProfileRepository.save(seller);
    }
}
