package com.closiq.inventory.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SellerInventoryResponse {

    String productId;
    List<VariantInventory> variants;

    @Value
    @Builder
    public static class VariantInventory {
        String variantId;
        String size;
        int quantity;
        boolean available;
        int bookedDates;
    }
}
