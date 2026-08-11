package com.closiq.payment.web;

import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.booking.service.CheckoutBatchService;
import com.closiq.payment.service.CheckoutService;
import com.closiq.payment.web.dto.CheckoutCalculateBatchRequest;
import com.closiq.payment.web.dto.CheckoutCalculateRequest;
import com.closiq.payment.web.dto.CheckoutCalculateResponse;
import com.closiq.payment.web.dto.CheckoutSessionResponse;
import com.closiq.payment.web.dto.InitiateCheckoutSessionRequest;
import com.closiq.payment.web.dto.PrepareCheckoutBatchRequest;
import com.closiq.payment.web.dto.PrepareCheckoutBatchResponse;
import com.closiq.payment.web.dto.ValidateCouponRequest;
import com.closiq.payment.web.dto.ValidateCouponResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout", description = "Price calculation and checkout session")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CheckoutBatchService checkoutBatchService;

    @PostMapping("/calculate")
    @Operation(summary = "Calculate rental pricing without creating a booking")
    public ResponseEntity<ApiResponse<CheckoutCalculateResponse>> calculate(
            @Valid @RequestBody CheckoutCalculateRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                checkoutService.calculate(body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/calculate-batch")
    @Operation(summary = "Calculate combined pricing for multiple bag items")
    public ResponseEntity<ApiResponse<CheckoutCalculateResponse>> calculateBatch(
            @Valid @RequestBody CheckoutCalculateBatchRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                checkoutService.calculateBatch(body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/batch/prepare")
    @Operation(summary = "Create holds for all bag items and prepare combined checkout")
    public ResponseEntity<ApiResponse<PrepareCheckoutBatchResponse>> prepareBatch(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PrepareCheckoutBatchRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                checkoutBatchService.prepare(principal.userId(), idempotencyKey, body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/coupons/validate")
    @Operation(summary = "Validate coupon against booking context")
    public ResponseEntity<ApiResponse<ValidateCouponResponse>> validateCoupon(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ValidateCouponRequest body,
            HttpServletRequest request) {

        UUID bookingId = body.getBookingId() != null ? UUID.fromString(body.getBookingId()) : null;
        return ResponseEntity.ok(ApiResponse.ok(
                checkoutService.validateCoupon(principal.userId(), body.getCouponCode(), bookingId),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/sessions")
    @Operation(summary = "Bind address and prepare checkout for payment")
    public ResponseEntity<ApiResponse<CheckoutSessionResponse>> initiateSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InitiateCheckoutSessionRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                checkoutService.initiateSession(principal.userId(), body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/sessions/{sessionId}/summary")
    @Operation(summary = "Checkout review summary")
    public ResponseEntity<ApiResponse<CheckoutSessionResponse>> sessionSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                checkoutService.getSummary(principal.userId(), sessionId),
                ClosiqRequestIdFilter.getRequestId(request)));
    }
}
