package com.closiq.seller.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class SubmitSellerApplicationRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    String businessName;

    @NotBlank
    @Pattern(regexp = "^(INDIVIDUAL|PROPRIETORSHIP|PARTNERSHIP|PRIVATE_LIMITED)$")
    String businessType;

    @NotBlank
    @Size(max = 50)
    String city;

    @Size(max = 500)
    String description;

    @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
    String gstNumber;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format")
    String panNumber;
}
