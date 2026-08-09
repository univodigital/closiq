package com.closiq.seller.web;

import com.closiq.common.security.RequiresSeller;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.seller.service.SellerBankAccountService;
import com.closiq.seller.web.dto.AddBankAccountRequest;
import com.closiq.seller.web.dto.BankAccountResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller/bank-accounts")
@RequiredArgsConstructor
@RequiresSeller
@Tag(name = "Seller Bank Accounts", description = "Payout bank accounts")
public class SellerBankAccountController {

    private final SellerBankAccountService sellerBankAccountService;

    @GetMapping
    @Operation(summary = "List bank accounts")
    public ResponseEntity<ApiResponse<List<BankAccountResponse>>> listBankAccounts(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        List<BankAccountResponse> accounts = sellerBankAccountService.listBankAccounts(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(accounts, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping
    @Operation(summary = "Add bank account")
    public ResponseEntity<ApiResponse<BankAccountResponse>> addBankAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddBankAccountRequest body,
            HttpServletRequest request) {

        BankAccountResponse account = sellerBankAccountService.addBankAccount(principal.userId(), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(account, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "Delete bank account")
    public ResponseEntity<Void> deleteBankAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID accountId) {

        sellerBankAccountService.deleteBankAccount(principal.userId(), accountId);
        return ResponseEntity.noContent().build();
    }
}
