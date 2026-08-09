package com.closiq.seller.web;

import com.closiq.common.security.RequiresSeller;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.seller.service.SellerProductService;
import com.closiq.seller.web.dto.ConfirmProductImageRequest;
import com.closiq.seller.web.dto.CreateSellerProductRequest;
import com.closiq.seller.web.dto.PresignedUploadResponse;
import com.closiq.seller.web.dto.ProductImageAttachResponse;
import com.closiq.seller.web.dto.ProductImageUploadUrlRequest;
import com.closiq.seller.web.dto.PublishProductResponse;
import com.closiq.seller.web.dto.SellerProductDetailResponse;
import com.closiq.seller.web.dto.SellerProductListItemResponse;
import com.closiq.seller.web.dto.SellerProductResponse;
import com.closiq.seller.web.dto.UpdateSellerProductRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller/products")
@RequiredArgsConstructor
@RequiresSeller
@Tag(name = "Seller Products", description = "Seller listing management")
public class SellerProductController {

    private final SellerProductService sellerProductService;

    @PostMapping
    @Operation(summary = "Create draft product listing")
    public ResponseEntity<ApiResponse<SellerProductResponse>> createProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateSellerProductRequest body,
            HttpServletRequest request) {

        SellerProductResponse response =
                sellerProductService.createProduct(principal.userId(), idempotencyKey, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping
    @Operation(summary = "List seller's own products")
    public ResponseEntity<ApiResponse<List<SellerProductListItemResponse>>> listProducts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {

        PagedResult<SellerProductListItemResponse> page =
                sellerProductService.listProducts(principal.userId(), status, pageToken, limit);
        return ResponseEntity.ok(ApiResponse.ok(
                page.getItems(),
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(page)));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get seller product detail")
    public ResponseEntity<ApiResponse<SellerProductDetailResponse>> getProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            HttpServletRequest request) {

        SellerProductDetailResponse response = sellerProductService.getProduct(principal.userId(), productId);
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PatchMapping("/{productId}")
    @Operation(summary = "Update product listing")
    public ResponseEntity<ApiResponse<SellerProductResponse>> updateProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateSellerProductRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                sellerProductService.updateProduct(principal.userId(), productId, body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Archive product listing")
    public ResponseEntity<Void> deleteProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId) {

        sellerProductService.deleteProduct(principal.userId(), productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/publish")
    @Operation(summary = "Publish draft listing to catalog")
    public ResponseEntity<ApiResponse<PublishProductResponse>> publishProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                sellerProductService.publishProduct(principal.userId(), productId),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/{productId}/images/upload-url")
    @Operation(summary = "Request presigned URL for product image upload")
    public ResponseEntity<ApiResponse<PresignedUploadResponse>> imageUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody ProductImageUploadUrlRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                sellerProductService.createImageUploadUrl(principal.userId(), productId, body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/{productId}/images")
    @Operation(summary = "Attach uploaded image to product")
    public ResponseEntity<ApiResponse<ProductImageAttachResponse>> confirmImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody ConfirmProductImageRequest body,
            HttpServletRequest request) {

        ProductImageAttachResponse response =
                sellerProductService.confirmImage(principal.userId(), productId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    @Operation(summary = "Remove product image")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @PathVariable UUID imageId) {

        sellerProductService.deleteImage(principal.userId(), productId, imageId);
        return ResponseEntity.noContent().build();
    }
}
