package com.closiq.catalog.service;

import com.closiq.catalog.mapper.ProductMapper;
import com.closiq.catalog.repository.ProductImageRepository;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.catalog.web.dto.ProductAvailabilityResponse;
import com.closiq.catalog.web.dto.ProductImageResponse;
import com.closiq.catalog.web.dto.ProductImagesWrapperResponse;
import com.closiq.catalog.web.dto.ProductReviewResponse;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.web.PagedResult;
import com.closiq.inventory.service.AvailabilityService;
import com.closiq.review.service.ReviewQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private static final String ACTIVE = ProductSpecifications.ACTIVE;

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductMapper productMapper;
    private final AvailabilityService availabilityService;
    private final ReviewQueryService reviewQueryService;

    @Transactional(readOnly = true)
    public ProductAvailabilityResponse getAvailability(String slugOrId, UUID variantId, LocalDate startDate, LocalDate endDate) {
        return availabilityService.getAvailability(slugOrId, variantId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public PagedResult<ProductReviewResponse> listReviews(String slugOrId, String pageToken, Integer limit, String sort) {
        return reviewQueryService.listProductReviews(slugOrId, pageToken, limit, sort);
    }

    @Transactional(readOnly = true)
    public ProductImagesWrapperResponse listImages(String slugOrId) {
        var product = productRepository.findActiveBySlugOrId(slugOrId, ACTIVE)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Product not found"));

        List<ProductImageResponse> images = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId())
                .stream()
                .map(productMapper::toImage)
                .toList();

        if (images.isEmpty() && product.getPrimaryImageUrl() != null) {
            images = List.of(ProductImageResponse.builder()
                    .url(product.getPrimaryImageUrl())
                    .sortOrder(0)
                    .build());
        }

        return ProductImagesWrapperResponse.builder().images(images).build();
    }
}
