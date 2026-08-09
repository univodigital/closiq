package com.closiq.admin.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminDashboardResponse {

    long totalUsers;
    long activeUsers;
    long suspendedUsers;
    long totalProducts;
    long activeProducts;
    long totalReviews;
    long publishedReviews;
    long pendingSellerApplications;
}
