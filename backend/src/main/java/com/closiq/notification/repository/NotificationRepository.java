package com.closiq.notification.repository;

import com.closiq.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndReadFalse(UUID userId);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.userId = :userId
            AND (n.expiresAt IS NULL OR n.expiresAt > :now)
            AND (:unreadOnly = false OR n.read = false)
            AND (n.createdAt < :beforeCreatedAt
                 OR (n.createdAt = :beforeCreatedAt AND n.id < :beforeId))
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<Notification> findFeedPage(
            @Param("userId") UUID userId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("now") Instant now,
            @Param("beforeCreatedAt") Instant beforeCreatedAt,
            @Param("beforeId") UUID beforeId,
            Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.read = true, n.readAt = :now
            WHERE n.userId = :userId AND n.read = false
            """)
    int markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);
}
