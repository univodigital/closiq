package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SellerWalletResponse {

    String sellerId;
    String currency;
    long availableBalance;
    long pendingBalance;
    long totalEarned;
    long totalWithdrawn;
    long minPayoutAmount;
    boolean payoutProviderConfigured;
    List<WalletTransactionResponse> transactions;
    List<PayoutMethodResponse> payoutMethods;
}
