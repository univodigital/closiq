package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SellerBookingHistoryResponse {

    List<SellerBookingListItemResponse> bookings;
    EarningsSummary summary;
    int page;
    int limit;
    long totalCount;
    int totalPages;

    @Value
    @Builder
    public static class EarningsSummary {
        long totalEarnings;
        long totalCommission;
        String currency;
    }
}
