package com.closiq.notification.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.web.PageBoundary;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.notification.domain.Notification;
import com.closiq.notification.repository.NotificationRepository;
import com.closiq.notification.web.dto.MarkAllReadResponse;
import com.closiq.notification.web.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public record NotificationListResult(PagedResult<NotificationResponse> page, long unreadCount) {
    }

    @Transactional(readOnly = true)
    public NotificationListResult list(UUID userId, String pageToken, Integer limit, Boolean unreadOnly) {
        int pageSize = normalizeLimit(limit);
        boolean unreadFilter = Boolean.TRUE.equals(unreadOnly);
        Instant now = Instant.now();
        PageBoundary boundary = PageTokenCodec.notificationBoundary(pageToken);

        List<Notification> rows = notificationRepository.findFeedPage(
                userId,
                unreadFilter,
                now,
                boundary.beforeCreatedAt(),
                boundary.beforeId(),
                PageRequest.of(0, pageSize + 1));

        boolean hasMore = rows.size() > pageSize;
        List<Notification> pageItems = hasMore ? rows.subList(0, pageSize) : rows;

        List<NotificationResponse> items = pageItems.stream().map(this::toResponse).toList();
        String nextPageToken = null;
        if (hasMore && !pageItems.isEmpty()) {
            Notification last = pageItems.get(pageItems.size() - 1);
            nextPageToken = PageTokenCodec.encodeNotification(
                    new PageTokenCodec.NotificationPageToken(last.getCreatedAt(), last.getId()));
        }

        long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);
        return new NotificationListResult(PagedResult.of(items, pageSize, hasMore, nextPageToken), unreadCount);
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId, boolean read) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Notification not found"));

        if (read) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        } else {
            notification.setRead(false);
            notification.setReadAt(null);
        }
        notificationRepository.save(notification);
        return toResponse(notification);
    }

    @Transactional
    public MarkAllReadResponse markAllRead(UUID userId) {
        int marked = notificationRepository.markAllRead(userId, Instant.now());
        return MarkAllReadResponse.builder().markedCount(marked).build();
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getNotificationType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .read(notification.isRead())
                .deepLink(notification.getDeepLink())
                .metadata(notification.getPayload())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        return Math.min(Math.max(limit, 1), 50);
    }
}
