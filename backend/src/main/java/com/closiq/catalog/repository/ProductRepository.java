package com.closiq.catalog.repository;

import com.closiq.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndDeletedAtIsNullAndStatus(UUID id, String status);

    Optional<Product> findBySlugAndDeletedAtIsNullAndStatus(String slug, String status);

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Product> findByIdempotencyKey(String idempotencyKey);

    boolean existsBySlugAndDeletedAtIsNull(String slug);

    long countBySellerProfileIdAndStatusAndDeletedAtIsNull(UUID sellerProfileId, String status);

    long countByCategoryIdAndDeletedAtIsNullAndStatus(UUID categoryId, String status);

    long countByCategoryIdAndDeletedAtIsNull(UUID categoryId);

    List<Product> findByFeaturedTrueAndDeletedAtIsNullAndStatusOrderByPublishedAtDesc(String status);

    List<Product> findByTrendingTrueAndDeletedAtIsNullAndStatusOrderByPublishedAtDesc(String status);

    List<Product> findByCategoryIdAndIdNotAndDeletedAtIsNullAndStatusOrderByAvgRatingDesc(
            UUID categoryId, UUID excludeId, String status, org.springframework.data.domain.Pageable pageable);

    long countByDeletedAtIsNull();

    long countByStatusAndDeletedAtIsNull(String status);

    default Optional<Product> findActiveBySlugOrId(String slugOrId, String status) {
        try {
            UUID id = UUID.fromString(slugOrId);
            return findByIdAndDeletedAtIsNullAndStatus(id, status);
        } catch (IllegalArgumentException ex) {
            return findBySlugAndDeletedAtIsNullAndStatus(slugOrId, status);
        }
    }
}
