package com.closiq.review.web;

import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.review.service.ReviewService;
import com.closiq.review.web.dto.CreateReviewRequest;
import com.closiq.review.web.dto.CreateReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product and seller reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Submit review after completed rental")
    public ResponseEntity<ApiResponse<CreateReviewResponse>> createReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateReviewRequest body,
            HttpServletRequest request) {

        CreateReviewResponse response = reviewService.createReview(principal.userId(), idempotencyKey, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }
}
