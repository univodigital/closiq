package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class WalletTransactionResponse {

    String id;
    String type;
    String label;
    long amount;
    String status;
    Instant createdAt;
}
