package com.closiq.seller.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.IdGenerator;
import com.closiq.user.domain.SellerProfile;
import com.closiq.seller.domain.PayoutRequest;
import com.closiq.seller.domain.Wallet;
import com.closiq.seller.domain.WalletTransaction;
import com.closiq.seller.repository.BankAccountRepository;
import com.closiq.seller.repository.PayoutRequestRepository;
import com.closiq.seller.repository.WalletRepository;
import com.closiq.seller.repository.WalletTransactionRepository;
import com.closiq.notification.service.NotificationDispatchService;
import com.closiq.seller.web.dto.PayoutMethodResponse;
import com.closiq.seller.web.dto.PayoutResponse;
import com.closiq.seller.web.dto.RequestPayoutRequest;
import com.closiq.seller.web.dto.SellerWalletResponse;
import com.closiq.seller.web.dto.WalletTransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerWalletService {

    private static final int MIN_PAYOUT = 500;
    private static final String INACTIVE = "INACTIVE";
    private static final String VERIFIED = "VERIFIED";

    private final SellerContextService sellerContextService;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PayoutRequestRepository payoutRequestRepository;
    private final NotificationDispatchService notificationDispatchService;

    @Transactional
    public SellerWalletResponse getWallet(UUID userId, int page, int limit) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Wallet wallet = getOrCreateWallet(seller);

        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(limit, 1), 100);
        Page<WalletTransaction> txns = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(
                wallet.getId(), PageRequest.of(pageIndex, pageSize));

        List<PayoutMethodResponse> payoutMethods = bankAccountRepository
                .findBySellerProfileIdAndStatusNotOrderByIsDefaultDescCreatedAtAsc(seller.getId(), INACTIVE)
                .stream()
                .map(account -> PayoutMethodResponse.builder()
                        .id(account.getId().toString())
                        .type("bank")
                        .label(buildBankLabel(account.getBankName(), account.getAccountNumberLast4()))
                        .isDefault(account.isDefault())
                        .verified(VERIFIED.equals(account.getStatus()))
                        .build())
                .toList();

        return SellerWalletResponse.builder()
                .sellerId(seller.getId().toString())
                .currency(wallet.getCurrencyCode())
                .availableBalance(wallet.getAvailableBalance())
                .pendingBalance(wallet.getPendingBalance())
                .totalEarned(wallet.getTotalEarned())
                .totalWithdrawn(wallet.getTotalWithdrawn())
                .minPayoutAmount(MIN_PAYOUT)
                .payoutProviderConfigured(false)
                .transactions(txns.getContent().stream().map(this::toTxnResponse).toList())
                .payoutMethods(payoutMethods)
                .build();
    }

    @Transactional
    public PayoutResponse requestPayout(UUID userId, RequestPayoutRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = payoutRequestRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                PayoutRequest payout = existing.get();
                return PayoutResponse.builder()
                        .payoutId(payout.getId().toString())
                        .status(payout.getStatus())
                        .amount(payout.getAmount())
                        .build();
            }
        }

        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Wallet wallet = getOrCreateWallet(seller);

        long amount = request.getAmount();
        if (amount < MIN_PAYOUT) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Minimum payout amount is ₹" + MIN_PAYOUT);
        }
        if (amount > wallet.getAvailableBalance()) {
            throw new ClosiqException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        UUID bankAccountId = UUID.fromString(request.getPayoutMethodId());
        var bankAccount = bankAccountRepository.findByIdAndSellerProfileId(bankAccountId, seller.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Bank account not found"));
        if (!VERIFIED.equals(bankAccount.getStatus())) {
            throw new ClosiqException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    "Add and verify a bank account before requesting payout");
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance() - amount);
        wallet.setPendingBalance(wallet.getPendingBalance() + amount);
        walletRepository.save(wallet);

        PayoutRequest payout = PayoutRequest.builder()
                .id(IdGenerator.uuidV7())
                .sellerProfile(seller)
                .bankAccount(bankAccount)
                .amount(amount)
                .status("PROCESSING")
                .idempotencyKey(blankToNull(idempotencyKey))
                .build();
        payoutRequestRepository.save(payout);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .txnType("DEBIT_PAYOUT")
                .amount(-amount)
                .balanceAfter(wallet.getAvailableBalance())
                .referenceType("PAYOUT")
                .referenceId(payout.getId().toString())
                .description("Payout to " + buildBankLabel(bankAccount.getBankName(), bankAccount.getAccountNumberLast4()))
                .status("PROCESSING")
                .build();
        walletTransactionRepository.save(txn);

        notificationDispatchService.sellerPayout(seller, amount, payout.getId());

        return PayoutResponse.builder()
                .payoutId(payout.getId().toString())
                .status(payout.getStatus())
                .amount(payout.getAmount())
                .build();
    }

    /**
     * Restores funds when an external payout provider reports failure.
     * Idempotent when payout is already marked FAILED.
     */
    @Transactional
    public void markPayoutFailed(UUID payoutId) {
        PayoutRequest payout = payoutRequestRepository.findById(payoutId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Payout not found"));
        if ("FAILED".equals(payout.getStatus())) {
            return;
        }
        if (!"PROCESSING".equals(payout.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Payout is not processing");
        }

        Wallet wallet = walletRepository.findBySellerProfileId(payout.getSellerProfile().getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Wallet not found"));

        long amount = payout.getAmount();
        wallet.setPendingBalance(Math.max(0, wallet.getPendingBalance() - amount));
        wallet.setAvailableBalance(wallet.getAvailableBalance() + amount);
        walletRepository.save(wallet);

        payout.setStatus("FAILED");
        payout.setProcessedAt(java.time.Instant.now());
        payoutRequestRepository.save(payout);

        walletTransactionRepository
                .findByReferenceTypeAndReferenceIdAndTxnType("PAYOUT", payoutId.toString(), "DEBIT_PAYOUT")
                .ifPresent(txn -> {
                    txn.setStatus("FAILED");
                    walletTransactionRepository.save(txn);
                });
    }

    private Wallet getOrCreateWallet(SellerProfile seller) {
        return walletRepository.findBySellerProfileId(seller.getId()).orElseGet(() -> {
            Wallet wallet = Wallet.builder()
                    .id(IdGenerator.uuidV7())
                    .sellerProfile(seller)
                    .availableBalance(0)
                    .pendingBalance(0)
                    .totalEarned(0)
                    .totalWithdrawn(0)
                    .currencyCode("INR")
                    .build();
            return walletRepository.save(wallet);
        });
    }

    private WalletTransactionResponse toTxnResponse(WalletTransaction txn) {
        return WalletTransactionResponse.builder()
                .id(String.valueOf(txn.getId()))
                .type(mapTxnType(txn.getTxnType()))
                .label(txn.getDescription())
                .amount(txn.getAmount())
                .status(txn.getStatus().toLowerCase())
                .createdAt(txn.getCreatedAt())
                .build();
    }

    private String mapTxnType(String txnType) {
        return switch (txnType) {
            case "CREDIT_EARNING" -> "earning";
            case "DEBIT_COMMISSION" -> "commission";
            case "DEBIT_PAYOUT" -> "payout";
            default -> txnType.toLowerCase();
        };
    }

    private String buildBankLabel(String bankName, String last4) {
        String bank = bankName != null ? bankName : "Bank";
        return bank + " ···" + last4;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
