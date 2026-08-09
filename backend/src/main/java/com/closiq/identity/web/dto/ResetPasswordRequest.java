package com.closiq.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class ResetPasswordRequest {

    String resetToken;

    String otpSessionId;

    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    String otp;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$", message = "Password must contain at least one uppercase letter and one digit")
    String newPassword;
}
