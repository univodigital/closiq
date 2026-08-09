package com.closiq.identity.web.dto;

import com.closiq.identity.domain.Gender;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class UserSummaryResponse {

    String id;
    String phone;
    String userCode;
    boolean phoneVerified;
    String alternatePhone;
    String email;
    boolean emailVerified;
    String alternateEmail;
    String firstName;
    String lastName;
    Gender gender;
    String username;
    String displayName;
    String avatarUrl;
    List<String> roles;
    Instant createdAt;
    SellerProfileStubResponse sellerProfile;
}
