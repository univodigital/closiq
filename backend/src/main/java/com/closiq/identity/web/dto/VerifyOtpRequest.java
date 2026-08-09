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

        @NotBlank(message = "First name is required")
        @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
        String firstName;

        @NotBlank(message = "Last name is required")
        @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
        String lastName;

        @Email(message = "Email must be valid")
        String email;
    }
}
