package com.closiq.catalog.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ProductFiltersResponse {

    List<FacetOption> occasions;
    List<FacetOption> sizes;
    PriceRange priceRange;
    List<FacetOption> cities;

    @Value
    @Builder
    public static class FacetOption {
        String slug;
        String name;
        String value;
        long count;
    }

    @Value
    @Builder
    public static class PriceRange {
        long min;
        long max;
    }
}
