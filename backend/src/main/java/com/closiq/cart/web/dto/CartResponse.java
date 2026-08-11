package com.closiq.cart.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class CartResponse {

    List<Item> items;

    @Value
    @Builder
    public static class Item {
        String productSlug;
        String variantSize;
        LocalDate rentalStartDate;
        LocalDate rentalEndDate;
    }
}
