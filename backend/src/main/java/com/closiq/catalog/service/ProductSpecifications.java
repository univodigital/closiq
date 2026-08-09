package com.closiq.catalog.service;

import com.closiq.catalog.domain.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ProductSpecifications {

    public static final String ACTIVE = "ACTIVE";

    private ProductSpecifications() {
    }

    public static Specification<Product> activeOnly() {
        return (root, query, cb) -> cb.and(
                cb.isNull(root.get("deletedAt")),
                cb.equal(root.get("status"), ACTIVE));
    }

    public static Specification<Product> withOccasionSlugs(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Object, Object> category = root.join("category", JoinType.INNER);
            return category.get("slug").in(slugs);
        };
    }

    public static Specification<Product> withCategoryId(UUID categoryId) {
        if (categoryId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> withSizes(List<String> sizes) {
        if (sizes == null || sizes.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Object, Object> variants = root.join("variants", JoinType.INNER);
            return cb.and(
                    variants.get("variantLabel").in(sizes),
                    cb.equal(variants.get("status"), ACTIVE));
        };
    }

    public static Specification<Product> withPriceRange(Long minPrice, Long maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("pricePerDay"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("pricePerDay"), maxPrice));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Product> withCity(String city) {
        if (city == null || city.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("city")), city.toLowerCase());
    }

    public static Specification<Product> featuredOnly(Boolean featured) {
        if (featured == null || !featured) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.isTrue(root.get("featured"));
    }

    public static Specification<Product> trendingOnly(Boolean trending) {
        if (trending == null || !trending) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.isTrue(root.get("trending"));
    }

    public static Specification<Product> searchQuery(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + queryText.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }

    public static Specification<Product> createdBefore(java.time.Instant beforeCreatedAt, UUID beforeId) {
        return (root, query, cb) -> cb.or(
                cb.lessThan(root.get("createdAt"), beforeCreatedAt),
                cb.and(
                        cb.equal(root.get("createdAt"), beforeCreatedAt),
                        cb.lessThan(root.get("id"), beforeId)));
    }

    public static Specification<Product> withAudience(String audience) {
        if (audience == null || audience.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("audience"), audience.toLowerCase());
    }

    public static Specification<Product> withGarmentType(String garmentType) {
        if (garmentType == null || garmentType.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("garmentType"), garmentType);
    }

    public static Specification<Product> combine(ProductFilter filter) {
        return activeOnly()
                .and(withOccasionSlugs(filter.occasionSlugs()))
                .and(withCategoryId(filter.categoryId()))
                .and(withSizes(filter.sizes()))
                .and(withPriceRange(filter.minPrice(), filter.maxPrice()))
                .and(withCity(filter.city()))
                .and(featuredOnly(filter.featured()))
                .and(trendingOnly(filter.trending()))
                .and(withAudience(filter.audience()))
                .and(withGarmentType(filter.garmentType()))
                .and(searchQuery(filter.query()))
                .and(createdBefore(filter.beforeCreatedAt(), filter.beforeId()));
    }

    public record ProductFilter(
            List<String> occasionSlugs,
            UUID categoryId,
            List<String> sizes,
            Long minPrice,
            Long maxPrice,
            String city,
            Boolean featured,
            Boolean trending,
            String audience,
            String garmentType,
            String query,
            java.time.Instant beforeCreatedAt,
            UUID beforeId) {
    }
}
