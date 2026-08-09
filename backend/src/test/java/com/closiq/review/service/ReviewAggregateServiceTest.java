package com.closiq.review.service;

import com.closiq.catalog.domain.Product;
import com.closiq.review.domain.ReviewStatus;
import com.closiq.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewAggregateServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private com.closiq.catalog.repository.ProductRepository productRepository;

    @Mock
    private com.closiq.user.repository.SellerProfileRepository sellerProfileRepository;

    @InjectMocks
    private ReviewAggregateService aggregateService;

    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
    }

    @Test
    void refreshProductAggregate_updatesCountAndAverage() {
        Product product = Product.builder().id(productId).reviewCount(0).build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.PUBLISHED)).thenReturn(2L);
        when(reviewRepository.averageProductRating(productId)).thenReturn(new BigDecimal("4.500000"));

        aggregateService.refreshProductAggregate(productId);

        assertThat(product.getReviewCount()).isEqualTo(2);
        assertThat(product.getAvgRating()).isEqualByComparingTo("4.5");
        verify(productRepository).save(product);
    }
}
