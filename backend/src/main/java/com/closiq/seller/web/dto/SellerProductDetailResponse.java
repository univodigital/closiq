package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class SellerProductDetailResponse {

    String id;
    String productCode;
    String slug;
    String title;
    String description;
    String status;
    long pricePerDay;
    long deposit;
    String city;
    String primaryImageUrl;
    List<String> imageUrls;
    List<VariantSummary> variants;
    String categoryId;
    String occasion;
    String audience;
    String garmentType;
    short minRentalDays;
    Short maxRentalDays;
    boolean includesTrial;
    Instant createdAt;
    Instant publishedAt;

    @Value
    @Builder
    public static class VariantSummary {
        String id;
        String size;
        String status;
        int availableQuantity;
    }
}
