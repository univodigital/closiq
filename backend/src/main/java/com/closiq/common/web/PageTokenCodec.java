package com.closiq.common.web;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.web.ResponseMeta.PaginationMeta;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

@UtilityClass
public class PageTokenCodec {

    public record WishlistPageToken(Instant createdAt, UUID productId) {
        public PageBoundary toBoundary() {
            return PageBoundary.before(createdAt, productId);
        }
    }

    public record ProductPageToken(Instant createdAt, UUID id) {
        public PageBoundary toBoundary() {
            return PageBoundary.before(createdAt, id);
        }
    }

    public record BookingPageToken(Instant createdAt, UUID id) {
        public PageBoundary toBoundary() {
            return PageBoundary.before(createdAt, id);
        }
    }

    public record PaymentPageToken(Instant createdAt, UUID id) {
        public PageBoundary toBoundary() {
            return PageBoundary.before(createdAt, id);
        }
    }

    public record ReviewPageToken(Instant createdAt, UUID id) {
        public PageBoundary toBoundary() {
            return PageBoundary.before(createdAt, id);
        }
    }

    public record ReviewRatingPageToken(short rating, UUID id) {
        public static ReviewRatingPageToken firstPage() {
            return new ReviewRatingPageToken(Short.MAX_VALUE, PageBoundary.MAX_ID);
        }
    }

    public record NotificationPageToken(Instant createdAt, UUID id) {
        public PageBoundary toBoundary() {
            return PageBoundary.before(createdAt, id);
        }
    }

    public record UserPageToken(Instant createdAt, UUID id) {
        public PageBoundary toBoundary() {
            return PageBoundary.before(createdAt, id);
        }
    }

    public record SellerBookingPageToken(LocalDate rentalStartDate, UUID id) {
    }

    public PageBoundary productBoundary(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return PageBoundary.now();
        }
        return decodeProduct(pageToken).toBoundary();
    }

    public ReviewRatingPageToken reviewRatingPageToken(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return ReviewRatingPageToken.firstPage();
        }
        return decodeReviewRating(pageToken);
    }

    public SellerBookingPageToken sellerBookingPageToken(String pageToken, boolean ascending) {
        if (pageToken == null || pageToken.isBlank()) {
            return ascending
                    ? new SellerBookingPageToken(LocalDate.MIN, new UUID(0L, 0L))
                    : new SellerBookingPageToken(LocalDate.MAX, PageBoundary.MAX_ID);
        }
        return decodeSellerBooking(pageToken);
    }

    public PageBoundary userBoundary(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return PageBoundary.now();
        }
        return decodeUser(pageToken).toBoundary();
    }

    public PageBoundary bookingBoundary(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return PageBoundary.now();
        }
        return decodeBooking(pageToken).toBoundary();
    }

    public PageBoundary paymentBoundary(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return PageBoundary.now();
        }
        return decodePayment(pageToken).toBoundary();
    }

    public PageBoundary reviewBoundary(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return PageBoundary.now();
        }
        return decodeReview(pageToken).toBoundary();
    }

    public PageBoundary notificationBoundary(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return PageBoundary.now();
        }
        return decodeNotification(pageToken).toBoundary();
    }

    public PageBoundary wishlistBoundary(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return PageBoundary.now();
        }
        return decodeWishlist(pageToken).toBoundary();
    }

    public String encodePayment(PaymentPageToken token) {
        String raw = token.createdAt().toEpochMilli() + ":" + token.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public PaymentPageToken decodePayment(String pageToken) {
        return decodeInstantId(pageToken, PaymentPageToken::new);
    }

    public String encodeReview(ReviewPageToken token) {
        String raw = token.createdAt().toEpochMilli() + ":" + token.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public ReviewPageToken decodeReview(String pageToken) {
        return decodeInstantId(pageToken, ReviewPageToken::new);
    }

    public String encodeReviewRating(ReviewRatingPageToken token) {
        String raw = token.rating() + ":" + token.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public ReviewRatingPageToken decodeReviewRating(String pageToken) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(pageToken), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            return new ReviewRatingPageToken(Short.parseShort(parts[0]), UUID.fromString(parts[1]));
        } catch (Exception ex) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Invalid page token");
        }
    }

    public String encodeNotification(NotificationPageToken token) {
        String raw = token.createdAt().toEpochMilli() + ":" + token.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public NotificationPageToken decodeNotification(String pageToken) {
        return decodeInstantId(pageToken, NotificationPageToken::new);
    }

    public String encodeSellerBooking(SellerBookingPageToken token) {
        String raw = token.rentalStartDate() + ":" + token.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public SellerBookingPageToken decodeSellerBooking(String pageToken) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(pageToken), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            return new SellerBookingPageToken(LocalDate.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (Exception ex) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Invalid page token");
        }
    }

    public String encodeBooking(BookingPageToken token) {
        String raw = token.createdAt().toEpochMilli() + ":" + token.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public BookingPageToken decodeBooking(String pageToken) {
        return decodeInstantId(pageToken, BookingPageToken::new);
    }

    public String encodeProduct(ProductPageToken token) {
        String raw = token.createdAt().toEpochMilli() + ":" + token.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public ProductPageToken decodeProduct(String pageToken) {
        return decodeInstantId(pageToken, ProductPageToken::new);
    }

    public String encodeUser(UserPageToken token) {
        String raw = token.createdAt().toEpochMilli() + ":" + token.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public UserPageToken decodeUser(String pageToken) {
        return decodeInstantId(pageToken, UserPageToken::new);
    }

    public String encodeWishlist(WishlistPageToken token) {
        String raw = token.createdAt().toEpochMilli() + ":" + token.productId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public WishlistPageToken decodeWishlist(String pageToken) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(pageToken), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            return new WishlistPageToken(Instant.ofEpochMilli(Long.parseLong(parts[0])), UUID.fromString(parts[1]));
        } catch (Exception ex) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Invalid page token");
        }
    }

    public PaginationMeta toPaginationMeta(PagedResult<?> page) {
        return PaginationMeta.builder()
                .type("page")
                .limit(page.getLimit())
                .nextPageToken(page.getNextPageToken())
                .prevPageToken(page.getPrevPageToken())
                .hasMore(page.isHasMore())
                .totalCount(null)
                .build();
    }

    private <T> T decodeInstantId(String pageToken, BiInstantUuid<T> factory) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(pageToken), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            return factory.apply(Instant.ofEpochMilli(Long.parseLong(parts[0])), UUID.fromString(parts[1]));
        } catch (Exception ex) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Invalid page token");
        }
    }

    @FunctionalInterface
    private interface BiInstantUuid<T> {
        T apply(Instant createdAt, UUID id);
    }
}
