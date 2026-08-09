package com.closiq.user.web.dto;

import com.closiq.identity.domain.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.util.List;

@Value
public class UpdateProfileRequest {

    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    String firstName;

    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    String lastName;

    Gender gender;

    @Email(message = "Email must be valid")
    String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Alternate phone must be 10–15 digits")
    String alternatePhone;

    @Email(message = "Alternate email must be valid")
    String alternateEmail;

    String avatarUrl;

    PreferencesPatch preferences;

    @Value
    public static class PreferencesPatch {

        @Pattern(regexp = "^(XS|S|M|L|XL|XXL)$", message = "Size must be one of XS, S, M, L, XL, XXL")
        String size;

        List<String> occasions;
    }
}
