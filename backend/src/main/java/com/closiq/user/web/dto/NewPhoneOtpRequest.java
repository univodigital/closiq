package com.closiq.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Value;

@Value
public class NewPhoneOtpRequest {

    @NotBlank
    @Pattern(regexp = "^(\\+91[6-9]\\d{9}|[6-9]\\d{9})$")
    String newPhone;
}
