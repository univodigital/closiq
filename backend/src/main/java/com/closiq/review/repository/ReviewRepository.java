package com.closiq.review.repository;

import com.closiq.review.domain.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByIdempotencyKey(String idempotencyKey);

    Optional<Review> findByBookingIdAndAuthorId(UUID bookingId, UUID authorId);

    boolean existsByBookingIdAndAuthorId(UUID bookingId, UUID authorId);

    @Query("""
            SELECT r FROM Review r
            WHERE r.productId = :productId
            AND r.status = 'PUBLISHED'
            AND (r.createdAt < :beforeCreatedAt
                 OR (r.createdAt = :beforeCreatedAt AND r.id < :beforeId))
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<Review> findPublishedByProductPage(
            @Param("productId") UUID productId,
            @Param("beforeCreatedAt") Instant beforeCreatedAt,
            @Param("beforeId") UUID beforeId,
            Pageable pageable);

    @Query("""
            SELECT r FROM Review r
            WHERE r.productId = :productId
            AND r.status = 'PUBLISHED'
            AND (r.productRating < :beforeRating
                 OR (r.productRating = :beforeRating AND r.id < :beforeId))
            ORDER BY r.productRating DESC, r.id DESC
            """)
    List<Review> findPublishedByProductRatingPage(
            @Param("productId") UUID productId,
            @Param("beforeRating") Short beforeRating,
            @Param("beforeId") UUID beforeId,
            Pageable pageable);

    @Query("""
            SELECT r FROM Review r
            WHERE r.sellerProfileId = :sellerProfileId
            AND r.status = 'PUBLISHED'
            AND (r.createdAt < :beforeCreatedAt
                 OR (r.createdAt = :beforeCreatedAt AND r.id < :beforeId))
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<Review> findPublishedBySellerPage(
            @Param("sellerProfileId") UUID sellerProfileId,
            @Param("beforeCreatedAt") Instant beforeCreatedAt,
            @Param("beforeId") UUID beforeId,
            Pageable pageable);

    long countByProductIdAndStatus(UUID productId, String status);

    long countBySellerProfileIdAndStatus(UUID sellerProfileId, String status);

    @Query("""
            SELECT COALESCE(AVG(r.productRating), 0)
            FROM Review r
            WHERE r.productId = :productId AND r.status = 'PUBLISHED'
            """)
    BigDecimal averageProductRating(@Param("productId") UUID productId);

    @Query("""
            SELECT COALESCE(AVG(COALESCE(r.sellerRating, r.productRating)), 0)
            FROM Review r
            WHERE r.sellerProfileId = :sellerProfileId AND r.status = 'PUBLISHED'
            """)
    BigDecimal averageSellerRating(@Param("sellerProfileId") UUID sellerProfileId);

    long countByStatus(String status);

    @Query("""
            SELECT r FROM Review r
            WHERE (:status IS NULL OR r.status = :status)
            AND (r.createdAt < :beforeCreatedAt
                 OR (r.createdAt = :beforeCreatedAt AND r.id < :beforeId))
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<Review> findAdminPage(
            @Param("status") String status,
            @Param("beforeCreatedAt") Instant beforeCreatedAt,
            @Param("beforeId") UUID beforeId,
            Pageable pageable);
}
