package com.closiq.identity.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class VerifyOtpRequest {

    @NotBlank(message = "OTP session ID is required")
    String otpSessionId;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    String otp;

    @NotBlank(message = "Purpose is required")
    @Pattern(regexp = "^(REGISTER|LOGIN|RESET_PASSWORD)$", message = "Purpose must be REGISTER, LOGIN, or RESET_PASSWORD")
    String purpose;

    @Valid
    RegistrationProfileRequest profile;

    @Value
    public static class RegistrationProfileRequest {

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username may only contain letters, numbers, and underscores")
        String username;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$", message = "Password must contain at least one uppercase letter and one digit")
        String password;

        @Email(message = "Email must be valid")
        String email;
    }
}
