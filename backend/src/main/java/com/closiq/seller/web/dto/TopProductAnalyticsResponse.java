package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TopProductAnalyticsResponse {

    String productId;
    String title;
    long views;
    long bookings;
}
