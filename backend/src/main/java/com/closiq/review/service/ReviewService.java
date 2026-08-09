package com.closiq.review.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.domain.BookingItem;
import com.closiq.booking.repository.BookingItemRepository;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.catalog.domain.Product;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.IdGenerator;
import com.closiq.review.domain.Review;
import com.closiq.review.domain.ReviewImage;
import com.closiq.review.domain.ReviewStatus;
import com.closiq.review.repository.ReviewImageRepository;
import com.closiq.review.repository.ReviewRepository;
import com.closiq.review.web.dto.CreateReviewRequest;
import com.closiq.review.web.dto.CreateReviewResponse;
import com.closiq.seller.domain.MediaAsset;
import com.closiq.seller.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final Set<String> REVIEWABLE_STATUSES = Set.of(
            BookingStatus.COMPLETED, BookingStatus.DEPOSIT_REFUNDED);

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final ProductRepository productRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ReviewAggregateService aggregateService;

    @Transactional
    public CreateReviewResponse createReview(UUID customerId, String idempotencyKey, CreateReviewRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key header is required");
        }

        var existingByKey = reviewRepository.findByIdempotencyKey(idempotencyKey);
        if (existingByKey.isPresent()) {
            Review review = existingByKey.get();
            if (!review.getAuthorId().equals(customerId) || !matchesRequest(review, request)) {
                throw new ClosiqException(ErrorCode.IDEMPOTENCY_CONFLICT);
            }
            return toResponse(review);
        }

        Booking booking = resolveBooking(customerId, request.getBookingId());
        if (!REVIEWABLE_STATUSES.contains(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Booking is not eligible for review");
        }

        if (reviewRepository.existsByBookingIdAndAuthorId(booking.getId(), customerId)) {
            throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "Review already submitted for this booking");
        }

        BookingItem item = bookingItemRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking item not found"));

        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Product not found"));

        List<UUID> photoIds = parsePhotoIds(request.getPhotoUploadIds());
        validatePhotos(customerId, photoIds);

        Instant now = Instant.now();
        Review review = Review.builder()
                .id(IdGenerator.uuidV7())
                .authorId(customerId)
                .productId(product.getId())
                .sellerProfileId(booking.getSellerProfileId())
                .bookingId(booking.getId())
                .productRating(request.getProductRating().shortValue())
                .sellerRating(request.getSellerRating() != null ? request.getSellerRating().shortValue() : null)
                .body(request.getComment())
                .status(ReviewStatus.PUBLISHED)
                .verifiedPurchase(true)
                .idempotencyKey(idempotencyKey)
                .publishedAt(now)
                .build();
        reviewRepository.save(review);

        attachPhotos(review.getId(), photoIds);
        aggregateService.refreshProductAggregate(product.getId());
        if (booking.getSellerProfileId() != null) {
            aggregateService.refreshSellerAggregate(booking.getSellerProfileId());
        }

        return toResponse(review);
    }

    private void attachPhotos(UUID reviewId, List<UUID> photoIds) {
        short order = 0;
        for (UUID mediaId : photoIds) {
            reviewImageRepository.save(ReviewImage.builder()
                    .id(IdGenerator.uuidV7())
                    .reviewId(reviewId)
                    .mediaAssetId(mediaId)
                    .sortOrder(order++)
                    .build());
            mediaAssetRepository.findById(mediaId).ifPresent(asset -> {
                asset.setStatus("ATTACHED");
                mediaAssetRepository.save(asset);
            });
        }
    }

    private void validatePhotos(UUID customerId, List<UUID> photoIds) {
        for (UUID mediaId : photoIds) {
            MediaAsset asset = mediaAssetRepository.findByIdAndUploadedById(mediaId, customerId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.VALIDATION_ERROR, "Invalid photo upload id"));
            if (!"UPLOADED".equals(asset.getStatus()) && !"ATTACHED".equals(asset.getStatus())) {
                throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Photo upload is not available");
            }
        }
    }

    private List<UUID> parsePhotoIds(List<String> photoUploadIds) {
        if (photoUploadIds == null || photoUploadIds.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>();
        for (String raw : photoUploadIds) {
            try {
                ids.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ex) {
                throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Invalid photo upload id");
            }
        }
        return ids;
    }

    private boolean matchesRequest(Review review, CreateReviewRequest request) {
        UUID bookingId = resolveBookingIdOnly(request.getBookingId());
        return review.getBookingId().equals(bookingId)
                && review.getProductRating() == request.getProductRating().shortValue()
                && sellerRatingMatches(review.getSellerRating(), request.getSellerRating())
                && nullableEquals(review.getBody(), request.getComment());
    }

    private boolean sellerRatingMatches(Short stored, Integer requested) {
        if (requested == null) {
            return stored == null;
        }
        return stored != null && stored == requested.shortValue();
    }

    private boolean nullableEquals(String a, String b) {
        if (a == null) {
            return b == null || b.isBlank();
        }
        return a.equals(b);
    }

    private Booking resolveBooking(UUID customerId, String bookingIdOrNumber) {
        if (bookingIdOrNumber.startsWith("VST-RNT-") || bookingIdOrNumber.startsWith("BK-")) {
            return bookingRepository.findByRentalNumberAndCustomerId(bookingIdOrNumber, customerId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        }
        if (bookingIdOrNumber.startsWith("VST-ORD-")) {
            return bookingRepository.findByOrderNumberAndCustomerId(bookingIdOrNumber, customerId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        }
        try {
            UUID id = UUID.fromString(bookingIdOrNumber);
            return bookingRepository.findByIdAndCustomerId(id, customerId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        } catch (IllegalArgumentException ex) {
            throw new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found");
        }
    }

    private UUID resolveBookingIdOnly(String bookingIdOrNumber) {
        if (bookingIdOrNumber.startsWith("VST-RNT-") || bookingIdOrNumber.startsWith("BK-")) {
            return bookingRepository.findByRentalNumber(bookingIdOrNumber)
                    .map(Booking::getId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        }
        if (bookingIdOrNumber.startsWith("VST-ORD-")) {
            return bookingRepository.findByOrderNumber(bookingIdOrNumber)
                    .map(Booking::getId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        }
        try {
            return UUID.fromString(bookingIdOrNumber);
        } catch (IllegalArgumentException ex) {
            throw new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found");
        }
    }

    private CreateReviewResponse toResponse(Review review) {
        return CreateReviewResponse.builder()
                .reviewId(review.getId())
                .productId(review.getProductId())
                .productRating(review.getProductRating())
                .sellerRating(review.getSellerRating() != null ? review.getSellerRating().intValue() : null)
                .comment(review.getBody())
                .verifiedRental(review.isVerifiedPurchase())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
