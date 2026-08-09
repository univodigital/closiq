package com.closiq.user.web.dto;

import com.closiq.identity.domain.Gender;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class UserProfileResponse {

    String id;
    String userCode;
    String phone;
    boolean phoneVerified;
    String alternatePhone;
    String email;
    boolean emailVerified;
    String alternateEmail;
    String firstName;
    String lastName;
    Gender gender;
    String displayName;
    String avatarUrl;
    List<String> roles;
    Instant createdAt;
    UserPreferencesResponse preferences;
    SellerProfileResponse sellerProfile;
}
