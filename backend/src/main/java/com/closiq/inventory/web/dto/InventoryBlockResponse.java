package com.closiq.inventory.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class InventoryBlockResponse {

    String id;
    String productId;
    String variantId;
    LocalDate startDate;
    LocalDate endDate;
    String reason;
    String status;
}
