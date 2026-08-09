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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerWalletService {

    private static final int MIN_PAYOUT = 500;
    private static final String INACTIVE = "INACTIVE";

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
                        .build())
                .toList();

        return SellerWalletResponse.builder()
                .sellerId(seller.getId().toString())
                .currency(wallet.getCurrencyCode())
                .availableBalance(wallet.getAvailableBalance())
                .pendingBalance(wallet.getPendingBalance())
                .totalEarned(wallet.getTotalEarned())
                .totalWithdrawn(wallet.getTotalWithdrawn())
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
        if (amount > wallet.getAvailableBalance()) {
            throw new ClosiqException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        UUID bankAccountId = UUID.fromString(request.getPayoutMethodId());
        var bankAccount = bankAccountRepository.findByIdAndSellerProfileId(bankAccountId, seller.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Bank account not found"));

        wallet.setAvailableBalance(wallet.getAvailableBalance() - amount);
        wallet.setTotalWithdrawn(wallet.getTotalWithdrawn() + amount);
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
                .status("COMPLETED")
                .build();
        walletTransactionRepository.save(txn);

        notificationDispatchService.sellerPayout(seller, amount, payout.getId());

        return PayoutResponse.builder()
                .payoutId(payout.getId().toString())
                .status(payout.getStatus())
                .amount(payout.getAmount())
                .build();
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
