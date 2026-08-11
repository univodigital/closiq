package com.closiq.admin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class RejectSellerApplicationRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(min = 10, max = 2000, message = "Rejection reason must be between 10 and 2000 characters")
    String reason;
}
