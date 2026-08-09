package com.closiq.seller.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Value;

@Value
public class KycUploadUrlRequest {

    @NotBlank
    @Pattern(regexp = "^(PAN|ADDRESS_PROOF|GST_CERTIFICATE|BANK_STATEMENT)$")
    String documentType;

    @NotBlank
    @Pattern(regexp = "^(application/pdf|image/jpeg|image/png)$")
    String contentType;

    @NotBlank
    String fileName;
}
