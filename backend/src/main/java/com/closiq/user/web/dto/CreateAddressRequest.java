package com.closiq.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class CreateAddressRequest {

    @NotBlank(message = "Label is required")
    @Pattern(regexp = "^(Home|Office|Other)$", message = "Label must be Home, Office, or Other")
    @Size(max = 20, message = "Label must be at most 20 characters")
    String label;

    @NotBlank(message = "Line 1 is required")
    @Size(max = 100, message = "Line 1 must be at most 100 characters")
    String line1;

    @Size(max = 100, message = "Line 2 must be at most 100 characters")
    String line2;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City must be at most 50 characters")
    String city;

    @NotBlank(message = "State is required")
    @Size(max = 50, message = "State must be at most 50 characters")
    String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^\\d{6}$", message = "Pincode must be 6 digits")
    String pincode;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Mobile number must be 10–15 digits")
    String phone;

    Boolean isDefault;
}
