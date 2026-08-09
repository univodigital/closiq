package com.closiq.seller.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class AddBankAccountRequest {

    @NotBlank
    @Size(max = 100)
    String accountHolderName;

    @NotBlank
    @Pattern(regexp = "^\\d{9,18}$", message = "Account number must be 9-18 digits")
    String accountNumber;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code")
    String ifscCode;

    String bankName;
}
