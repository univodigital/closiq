package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SellerAnalyticsResponse {

    String period;
    long views;
    long uniqueVisitors;
    long bookings;
    double conversionRate;
    long revenue;
    String currency;
    List<TopProductAnalyticsResponse> topProducts;
}
