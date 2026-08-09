package com.closiq.seller.web;

import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.seller.service.SellerApplicationService;
import com.closiq.seller.web.dto.ConfirmKycDocumentRequest;
import com.closiq.seller.web.dto.KycDocumentSummaryResponse;
import com.closiq.seller.web.dto.KycUploadUrlRequest;
import com.closiq.seller.web.dto.PresignedUploadResponse;
import com.closiq.seller.web.dto.SellerApplicationDetailResponse;
import com.closiq.seller.web.dto.SellerApplicationSubmitResponse;
import com.closiq.seller.web.dto.SubmitSellerApplicationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/applications")
@RequiredArgsConstructor
@Tag(name = "Seller Applications", description = "Become a seller — application and KYC")
public class SellerApplicationController {

    private final SellerApplicationService sellerApplicationService;

    @PostMapping
    @Operation(summary = "Submit seller application")
    public ResponseEntity<ApiResponse<SellerApplicationSubmitResponse>> submitApplication(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubmitSellerApplicationRequest request,
            HttpServletRequest httpRequest) {

        SellerApplicationSubmitResponse response =
                sellerApplicationService.submitApplication(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(httpRequest)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get seller application status")
    public ResponseEntity<ApiResponse<SellerApplicationDetailResponse>> getMyApplication(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        SellerApplicationDetailResponse response =
                sellerApplicationService.getMyApplication(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(httpRequest)));
    }

    @PostMapping("/me/kyc-documents/upload-url")
    @Operation(summary = "Get presigned URL for KYC document upload")
    public ResponseEntity<ApiResponse<PresignedUploadResponse>> createKycUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody KycUploadUrlRequest request,
            HttpServletRequest httpRequest) {

        PresignedUploadResponse response =
                sellerApplicationService.createKycUploadUrl(principal.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(httpRequest)));
    }

    @PostMapping("/me/kyc-documents/confirm")
    @Operation(summary = "Confirm KYC document upload")
    public ResponseEntity<ApiResponse<KycDocumentSummaryResponse>> confirmKycDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ConfirmKycDocumentRequest request,
            HttpServletRequest httpRequest) {

        KycDocumentSummaryResponse response =
                sellerApplicationService.confirmKycDocument(principal.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(httpRequest)));
    }
}
