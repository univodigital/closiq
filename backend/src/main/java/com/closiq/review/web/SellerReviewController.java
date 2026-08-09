package com.closiq.review.web;

import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.review.service.ReviewQueryService;
import com.closiq.review.web.dto.SellerReviewsPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "Sellers", description = "Public seller profile data")
public class SellerReviewController {

    private final ReviewQueryService reviewQueryService;

    @GetMapping("/{sellerId}/reviews")
    @Operation(summary = "Paginated seller reviews with aggregate rating")
    public ResponseEntity<ApiResponse<SellerReviewsPageResponse>> sellerReviews(
            @PathVariable UUID sellerId,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false, defaultValue = "createdAt:desc") String sort,
            HttpServletRequest request) {

        var result = reviewQueryService.listSellerReviews(sellerId, pageToken, limit, sort);
        SellerReviewsPageResponse data = SellerReviewsPageResponse.builder()
                .reviews(result.getPage().getItems())
                .aggregate(result.getAggregate())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(
                data,
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(result.getPage())));
    }
}
