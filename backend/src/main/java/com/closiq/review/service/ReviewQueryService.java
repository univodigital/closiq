package com.closiq.review.service;

import com.closiq.catalog.repository.ProductRepository;
import com.closiq.catalog.service.ProductSpecifications;
import com.closiq.catalog.web.dto.ProductReviewResponse;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.web.PageBoundary;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.review.domain.Review;
import com.closiq.review.domain.ReviewImage;
import com.closiq.review.domain.ReviewStatus;
import com.closiq.review.repository.ReviewImageRepository;
import com.closiq.review.repository.ReviewRepository;
import com.closiq.review.web.dto.ReviewAggregateResponse;
import com.closiq.review.web.dto.SellerReviewsResult;
import com.closiq.seller.domain.MediaAsset;
import com.closiq.seller.repository.MediaAssetRepository;
import com.closiq.storage.FileStorageService;
import com.closiq.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewQueryService {

    private static final String ACTIVE = ProductSpecifications.ACTIVE;

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ProductRepository productRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final UserProfileRepository userProfileRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public PagedResult<ProductReviewResponse> listProductReviews(
            String slugOrId, String pageToken, Integer limit, String sort) {
        var product = productRepository.findActiveBySlugOrId(slugOrId, ACTIVE)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Product not found"));

        int pageSize = normalizeLimit(limit);
        List<Review> reviews = fetchProductReviews(product.getId(), pageToken, pageSize, sort);
        return buildReviewPage(reviews, pageSize, sort);
    }

    @Transactional(readOnly = true)
    public SellerReviewsResult listSellerReviews(
            UUID sellerId, String pageToken, Integer limit, String sort) {

        sellerProfileRepository.findById(sellerId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Seller not found"));

        int pageSize = normalizeLimit(limit);
        List<Review> reviews = fetchSellerReviews(sellerId, pageToken, pageSize);
        PagedResult<ProductReviewResponse> page = buildReviewPage(reviews, pageSize, sort);

        long totalCount = reviewRepository.countBySellerProfileIdAndStatus(sellerId, ReviewStatus.PUBLISHED);
        BigDecimal average = totalCount > 0
                ? reviewRepository.averageSellerRating(sellerId).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return SellerReviewsResult.builder()
                .page(page)
                .aggregate(ReviewAggregateResponse.builder()
                        .averageRating(average)
                        .totalCount(totalCount)
                        .build())
                .build();
    }

    private List<Review> fetchProductReviews(UUID productId, String pageToken, int pageSize, String sort) {
        if ("rating:desc".equalsIgnoreCase(sort)) {
            PageTokenCodec.ReviewRatingPageToken token = PageTokenCodec.reviewRatingPageToken(pageToken);
            return reviewRepository.findPublishedByProductRatingPage(
                    productId,
                    token.rating(),
                    token.id(),
                    PageRequest.of(0, pageSize + 1));
        }
        PageBoundary boundary = PageTokenCodec.reviewBoundary(pageToken);
        return reviewRepository.findPublishedByProductPage(
                productId,
                boundary.beforeCreatedAt(),
                boundary.beforeId(),
                PageRequest.of(0, pageSize + 1));
    }

    private List<Review> fetchSellerReviews(UUID sellerId, String pageToken, int pageSize) {
        PageBoundary boundary = PageTokenCodec.reviewBoundary(pageToken);
        return reviewRepository.findPublishedBySellerPage(
                sellerId,
                boundary.beforeCreatedAt(),
                boundary.beforeId(),
                PageRequest.of(0, pageSize + 1));
    }

    private PagedResult<ProductReviewResponse> buildReviewPage(List<Review> reviews, int pageSize, String sort) {
        boolean hasMore = reviews.size() > pageSize;
        List<Review> pageItems = hasMore ? reviews.subList(0, pageSize) : reviews;

        Map<UUID, UserProfile> profiles = userProfileRepository.findAllById(
                        pageItems.stream().map(Review::getAuthorId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        Map<UUID, List<String>> photoUrls = loadPhotoUrls(pageItems);

        List<ProductReviewResponse> items = pageItems.stream()
                .map(review -> toProductReviewResponse(review, profiles.get(review.getAuthorId()), photoUrls))
                .toList();

        String nextPageToken = null;
        if (hasMore && !pageItems.isEmpty()) {
            Review last = pageItems.get(pageItems.size() - 1);
            nextPageToken = "rating:desc".equalsIgnoreCase(sort)
                    ? PageTokenCodec.encodeReviewRating(new PageTokenCodec.ReviewRatingPageToken(last.getProductRating(), last.getId()))
                    : PageTokenCodec.encodeReview(new PageTokenCodec.ReviewPageToken(last.getCreatedAt(), last.getId()));
        }

        return PagedResult.of(items, pageSize, hasMore, nextPageToken);
    }

    private Map<UUID, List<String>> loadPhotoUrls(List<Review> reviews) {
        List<UUID> reviewIds = reviews.stream().map(Review::getId).toList();
        if (reviewIds.isEmpty()) {
            return Map.of();
        }

        return reviewIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> reviewImageRepository.findByReviewIdOrderBySortOrderAsc(id).stream()
                        .map(ReviewImage::getMediaAssetId)
                        .map(mediaAssetRepository::findById)
                        .filter(java.util.Optional::isPresent)
                        .map(java.util.Optional::get)
                        .map(this::toPublicUrl)
                        .toList()));
    }

    private String toPublicUrl(MediaAsset asset) {
        return fileStorageService.resolvePublicUrl(asset);
    }

    private ProductReviewResponse toProductReviewResponse(
            Review review, UserProfile profile, Map<UUID, List<String>> photoUrls) {

        return ProductReviewResponse.builder()
                .id(review.getId().toString())
                .rating(review.getProductRating())
                .comment(review.getBody())
                .customerDisplayName(maskDisplayName(profile))
                .photos(photoUrls.getOrDefault(review.getId(), List.of()))
                .createdAt(review.getCreatedAt())
                .verifiedRental(review.isVerifiedPurchase())
                .build();
    }

    private String maskDisplayName(UserProfile profile) {
        if (profile == null) {
            return "Verified Renter";
        }
        if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
            return profile.getDisplayName();
        }
        String lastInitial = profile.getLastName() != null && !profile.getLastName().isBlank()
                ? profile.getLastName().substring(0, 1) + "."
                : "";
        return profile.getFirstName() + (lastInitial.isBlank() ? "" : " " + lastInitial);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        return Math.min(Math.max(limit, 1), 50);
    }
}
