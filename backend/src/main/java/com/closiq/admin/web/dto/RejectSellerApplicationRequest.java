package com.closiq.admin.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class RejectSellerApplicationRequest {

    @NotBlank(message = "Rejection reason is required")
    String reason;
}
