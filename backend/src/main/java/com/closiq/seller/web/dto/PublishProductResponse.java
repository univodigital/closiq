package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class PublishProductResponse {

    UUID productId;
    String status;
    Instant publishedAt;
}
