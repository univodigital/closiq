package com.closiq.user.repository;

import com.closiq.user.domain.WishlistItem;
import com.closiq.user.domain.WishlistItemId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, WishlistItemId> {

    long countByIdUserId(UUID userId);

    boolean existsByIdUserIdAndIdProductId(UUID userId, UUID productId);

    @Query("""
            SELECT wi FROM WishlistItem wi
            JOIN FETCH wi.product p
            WHERE wi.id.userId = :userId
              AND (wi.createdAt < :beforeCreatedAt
                   OR (wi.createdAt = :beforeCreatedAt AND wi.id.productId < :beforeProductId))
            ORDER BY wi.createdAt DESC, wi.id.productId DESC
            """)
    List<WishlistItem> findPage(
            @Param("userId") UUID userId,
            @Param("beforeCreatedAt") Instant beforeCreatedAt,
            @Param("beforeProductId") UUID beforeProductId,
            Pageable pageable);
}
