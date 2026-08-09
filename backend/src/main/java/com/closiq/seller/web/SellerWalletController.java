package com.closiq.seller.web;

import com.closiq.common.security.RequiresSeller;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.seller.service.SellerWalletService;
import com.closiq.seller.web.dto.PayoutResponse;
import com.closiq.seller.web.dto.RequestPayoutRequest;
import com.closiq.seller.web.dto.SellerWalletResponse;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/wallet")
@RequiredArgsConstructor
@RequiresSeller
@Tag(name = "Seller Wallet", description = "Seller wallet and payouts")
public class SellerWalletController {

    private final SellerWalletService sellerWalletService;

    @GetMapping
    @Operation(summary = "Get wallet balances and transactions")
    public ResponseEntity<ApiResponse<SellerWalletResponse>> getWallet(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {

        SellerWalletResponse response = sellerWalletService.getWallet(principal.userId(), page, limit);
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/payouts")
    @Operation(summary = "Request payout to bank account")
    public ResponseEntity<ApiResponse<PayoutResponse>> requestPayout(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RequestPayoutRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest request) {

        PayoutResponse response = sellerWalletService.requestPayout(
                principal.userId(), body, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }
}
