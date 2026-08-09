package com.closiq.catalog.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ProductSummaryResponse {

    String id;
    String slug;
    String productCode;
    String title;
    String designer;
    List<String> images;
    long pricePerDay;
    long deposit;
    String currency;
    Double rating;
    int reviewCount;
    List<String> badges;
    boolean includesTrial;
    String city;
    String audience;
    String garmentType;
    boolean trending;
}
