package com.closiq.seller.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class RejectSellerBookingRequest {

    @NotBlank
    String reason;

    String comment;
}
