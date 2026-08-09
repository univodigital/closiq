package com.closiq.catalog.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ProductDetailResponse {

    String id;
    String slug;
    String productCode;
    String title;
    String designer;
    String description;
    String categoryId;
    String occasion;
    List<String> images;
    long pricePerDay;
    long deposit;
    String currency;
    List<ProductVariantResponse> variants;
    Double rating;
    int reviewCount;
    List<String> badges;
    String sellerId;
    String sellerName;
    String city;
    List<String> deliverablePincodes;
    boolean includesTrial;
    short trialDurationMinutes;
    short minRentalDays;
    Short maxRentalDays;
    String sizeGuide;
    String audience;
    String garmentType;
    boolean trending;
}
