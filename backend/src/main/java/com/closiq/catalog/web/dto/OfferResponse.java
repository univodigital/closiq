package com.closiq.catalog.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class OfferResponse {

    String id;
    String title;
    String description;
    String code;
    String discountType;
    long discountValue;
    Instant validUntil;
    String imageUrl;
}
