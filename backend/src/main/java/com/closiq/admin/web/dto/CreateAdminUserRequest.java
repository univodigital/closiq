package com.closiq.admin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Value;

import java.util.List;

@Value
public class CreateAdminUserRequest {

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+91[6-9]\\d{9}$", message = "Phone must be a valid Indian mobile number (+91XXXXXXXXXX)")
    String phone;

    @NotBlank(message = "First name is required")
    String firstName;

    @NotBlank(message = "Last name is required")
    String lastName;

    String email;

    List<String> roles;
}
