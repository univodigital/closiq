package com.closiq.notification.web;

import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.notification.service.NotificationService;
import com.closiq.notification.web.dto.MarkAllReadResponse;
import com.closiq.notification.web.dto.MarkNotificationReadRequest;
import com.closiq.notification.web.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification feed")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List in-app notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Boolean unreadOnly,
            HttpServletRequest request) {

        NotificationService.NotificationListResult result =
                notificationService.list(principal.userId(), pageToken, limit, unreadOnly);

        return ResponseEntity.ok(ApiResponse.okWithNotifications(
                result.page().getItems(),
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(result.page()),
                result.unreadCount()));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification read or unread")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID notificationId,
            @Valid @RequestBody MarkNotificationReadRequest body,
            HttpServletRequest request) {

        NotificationResponse response =
                notificationService.markRead(principal.userId(), notificationId, body.getRead());
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<MarkAllReadResponse>> markAllRead(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        MarkAllReadResponse response = notificationService.markAllRead(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }
}
