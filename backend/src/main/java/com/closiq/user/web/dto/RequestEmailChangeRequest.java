package com.closiq.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class RequestEmailChangeRequest {

    @NotBlank
    @Email
    String newEmail;
}
