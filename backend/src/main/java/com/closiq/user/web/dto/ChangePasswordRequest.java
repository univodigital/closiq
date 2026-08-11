package com.closiq.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class ChangePasswordRequest {

    @NotBlank
    String currentPassword;

    @NotBlank
    @Size(min = 8)
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$", message = "Password must contain an uppercase letter and a digit")
    String newPassword;

    @NotBlank
    String confirmPassword;
}
