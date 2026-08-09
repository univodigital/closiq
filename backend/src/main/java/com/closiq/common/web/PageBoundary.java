package com.closiq.common.web;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

/**
 * Keyset pagination upper bound. First page uses {@link Instant#now()} so query parameters
 * are always typed (PostgreSQL-safe).
 */
public final class PageBoundary {

    public static final UUID MAX_ID = new UUID(-1L, -1L);

    private final Instant beforeCreatedAt;
    private final UUID beforeId;

    private PageBoundary(Instant beforeCreatedAt, UUID beforeId) {
        this.beforeCreatedAt = beforeCreatedAt;
        this.beforeId = beforeId;
    }

    public static PageBoundary now() {
        return new PageBoundary(Instant.now(), MAX_ID);
    }

    public static PageBoundary before(Instant createdAt, UUID id) {
        return new PageBoundary(createdAt, id);
    }

    public Instant beforeCreatedAt() {
        return beforeCreatedAt;
    }

    public UUID beforeId() {
        return beforeId;
    }

    public <T> Specification<T> createdBefore() {
        return (root, query, cb) -> cb.or(
                cb.lessThan(root.get("createdAt"), beforeCreatedAt),
                cb.and(
                        cb.equal(root.get("createdAt"), beforeCreatedAt),
                        cb.lessThan(root.get("id"), beforeId)));
    }
}
