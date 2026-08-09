package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class SellerDashboardResponse {

    DashboardSummaryResponse summary;
    List<DashboardTaskResponse> tasks;
    List<Object> recentBookings;
}
