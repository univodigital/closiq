package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class SellerInventoryBlockListItemResponse {

    String id;
    String productId;
    String productTitle;
    String variantId;
    String variantSize;
    LocalDate startDate;
    LocalDate endDate;
    String reason;
    String status;
}
