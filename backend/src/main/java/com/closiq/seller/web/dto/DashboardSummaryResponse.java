package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DashboardSummaryResponse {

    long activeListings;
    long pendingBookings;
    long earningsThisMonth;
    String currency;
}
