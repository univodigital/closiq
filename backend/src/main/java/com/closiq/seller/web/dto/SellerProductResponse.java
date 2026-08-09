package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class SellerProductResponse {

    UUID id;
    String slug;
    String productCode;
    String title;
    String status;
    Long pricePerDay;
    Long deposit;
    String city;
}
