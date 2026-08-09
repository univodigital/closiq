package com.closiq.payment.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class RefundStatusResponse {

    String paymentId;
    String bookingId;
    List<RefundItem> refunds;

    @Value
    @Builder
    public static class RefundItem {
        String refundId;
        String type;
        long amount;
        String status;
        Instant initiatedAt;
        Instant processedAt;
        Instant expectedBy;
    }
}
