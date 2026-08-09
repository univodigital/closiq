package com.closiq.admin.service;

import com.closiq.admin.web.dto.AdminReviewListItemResponse;
import com.closiq.admin.web.dto.UpdateAdminReviewRequest;
import com.closiq.catalog.domain.Product;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.web.PageBoundary;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.review.domain.Review;
import com.closiq.review.domain.ReviewStatus;
import com.closiq.review.repository.ReviewRepository;
import com.closiq.review.service.ReviewAggregateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserProfileRepository userProfileRepository;
    private final ReviewAggregateService reviewAggregateService;

    @Transactional(readOnly = true)
    public PagedResult<AdminReviewListItemResponse> listReviews(String status, String pageToken, Integer limit) {
        int pageSize = clampLimit(limit);
        PageBoundary boundary = PageTokenCodec.reviewBoundary(pageToken);

        List<Review> reviews = reviewRepository.findAdminPage(
                blankToNull(status),
                boundary.beforeCreatedAt(),
                boundary.beforeId(),
                PageRequest.of(0, pageSize + 1));

        boolean hasMore = reviews.size() > pageSize;
        List<Review> page = hasMore ? reviews.subList(0, pageSize) : reviews;

        Map<UUID, String> authorNames = loadAuthorNames(page);
        Map<UUID, String> productTitles = loadProductTitles(page);

        List<AdminReviewListItemResponse> items = page.stream()
                .map(review -> toListItem(review, authorNames, productTitles))
                .toList();

        String nextPageToken = null;
        if (hasMore && !page.isEmpty()) {
            Review last = page.get(page.size() - 1);
            nextPageToken = PageTokenCodec.encodeReview(new PageTokenCodec.ReviewPageToken(last.getCreatedAt(), last.getId()));
        }

        return PagedResult.of(items, pageSize, hasMore, nextPageToken);
    }

    @Transactional
    public void deleteReview(UUID reviewId) {
        Review review = requireReview(reviewId);
        review.setStatus(ReviewStatus.HIDDEN);
        reviewRepository.save(review);
        refreshAggregates(review);
    }

    @Transactional
    public AdminReviewListItemResponse updateReview(UUID reviewId, UpdateAdminReviewRequest request) {
        Review review = requireReview(reviewId);

        if (request.getStatus() != null) {
            validateReviewStatus(request.getStatus());
            review.setStatus(request.getStatus());
            reviewRepository.save(review);
            refreshAggregates(review);
        }

        Map<UUID, String> authorNames = loadAuthorNames(List.of(review));
        Map<UUID, String> productTitles = loadProductTitles(List.of(review));
        return toListItem(review, authorNames, productTitles);
    }

    private AdminReviewListItemResponse toListItem(
            Review review,
            Map<UUID, String> authorNames,
            Map<UUID, String> productTitles) {

        return AdminReviewListItemResponse.builder()
                .id(review.getId().toString())
                .authorDisplayName(authorNames.getOrDefault(review.getAuthorId(), "Unknown user"))
                .productTitle(productTitles.getOrDefault(review.getProductId(), "Unknown product"))
                .productRating(review.getProductRating())
                .sellerRating(review.getSellerRating())
                .title(review.getTitle())
                .body(review.getBody())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .publishedAt(review.getPublishedAt())
                .build();
    }

    private void refreshAggregates(Review review) {
        reviewAggregateService.refreshProductAggregate(review.getProductId());
        if (review.getSellerProfileId() != null) {
            reviewAggregateService.refreshSellerAggregate(review.getSellerProfileId());
        }
    }

    private Map<UUID, String> loadAuthorNames(List<Review> reviews) {
        List<UUID> authorIds = reviews.stream().map(Review::getAuthorId).distinct().toList();
        if (authorIds.isEmpty()) {
            return Map.of();
        }
        return userProfileRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getDisplayName));
    }

    private Map<UUID, String> loadProductTitles(List<Review> reviews) {
        List<UUID> productIds = reviews.stream().map(Review::getProductId).distinct().toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getTitle));
    }

    private Review requireReview(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Review not found"));
    }

    private void validateReviewStatus(String status) {
        if (!List.of(ReviewStatus.PENDING, ReviewStatus.PUBLISHED, ReviewStatus.HIDDEN, ReviewStatus.FLAGGED)
                .contains(status)) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Invalid review status");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private int clampLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
