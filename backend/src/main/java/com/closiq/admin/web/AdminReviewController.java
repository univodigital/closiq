package com.closiq.admin.web;

import com.closiq.admin.service.AdminReviewService;
import com.closiq.admin.web.dto.AdminReviewListItemResponse;
import com.closiq.admin.web.dto.UpdateAdminReviewRequest;
import com.closiq.common.security.RequiresAdmin;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.common.web.ClosiqRequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@RequiresAdmin
@Tag(name = "Admin Reviews", description = "Review moderation")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @GetMapping
    @Operation(summary = "List all reviews")
    public ResponseEntity<ApiResponse<List<AdminReviewListItemResponse>>> listReviews(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {

        PagedResult<AdminReviewListItemResponse> page = adminReviewService.listReviews(status, pageToken, limit);
        return ResponseEntity.ok(ApiResponse.ok(
                page.getItems(),
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(page)));
    }

    @PatchMapping("/{reviewId}")
    @Operation(summary = "Update review status")
    public ResponseEntity<ApiResponse<AdminReviewListItemResponse>> updateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateAdminReviewRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                adminReviewService.updateReview(reviewId, body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Hide review")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID reviewId) {
        adminReviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
