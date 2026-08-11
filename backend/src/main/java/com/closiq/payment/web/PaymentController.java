package com.closiq.payment.web;

import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.payment.service.PaymentQueryService;
import com.closiq.payment.service.PaymentService;
import com.closiq.payment.web.dto.CreateBatchRazorpayOrderRequest;
import com.closiq.payment.web.dto.CreateRazorpayOrderRequest;
import com.closiq.payment.web.dto.CreateRazorpayOrderResponse;
import com.closiq.payment.web.dto.PaymentSummaryResponse;
import com.closiq.payment.web.dto.RefundStatusResponse;
import com.closiq.payment.web.dto.VerifyPaymentRequest;
import com.closiq.payment.web.dto.VerifyPaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay orders and payment verification")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentQueryService paymentQueryService;

    @PostMapping("/razorpay/orders")
    @Operation(summary = "Create Razorpay order for booking payment")
    public ResponseEntity<ApiResponse<CreateRazorpayOrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateRazorpayOrderRequest body,
            HttpServletRequest request) {

        CreateRazorpayOrderResponse response = paymentService.createRazorpayOrder(
                principal.userId(), idempotencyKey, body.getBookingId(), body.getCheckoutSessionId());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                response,                 ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/razorpay/orders/batch")
    @Operation(summary = "Create single Razorpay order for multi-item checkout batch")
    public ResponseEntity<ApiResponse<CreateRazorpayOrderResponse>> createBatchOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateBatchRazorpayOrderRequest body,
            HttpServletRequest request) {

        CreateRazorpayOrderResponse response = paymentService.createBatchRazorpayOrder(
                principal.userId(), idempotencyKey, body.getCheckoutBatchId());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/razorpay/verify")
    @Operation(summary = "Verify Razorpay payment and confirm booking")
    public ResponseEntity<ApiResponse<VerifyPaymentResponse>> verifyPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody VerifyPaymentRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.verifyPayment(principal.userId(), body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping
    @Operation(summary = "Customer payment history")
    public ResponseEntity<ApiResponse<List<PaymentSummaryResponse>>> listPayments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID bookingId,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {

        PagedResult<PaymentSummaryResponse> page = paymentQueryService.listPayments(
                principal.userId(), status, bookingId, pageToken, limit);

        return ResponseEntity.ok(ApiResponse.ok(
                page.getItems(),
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(page)));
    }

    @GetMapping("/{paymentId}/refund-status")
    @Operation(summary = "Refund status for a payment")
    public ResponseEntity<ApiResponse<RefundStatusResponse>> refundStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID paymentId,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                paymentQueryService.getRefundStatus(principal.userId(), paymentId),
                ClosiqRequestIdFilter.getRequestId(request)));
    }
}
