package com.closiq.admin.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class AdminUserDetailResponse {

    String id;
    String userCode;
    String phone;
    boolean phoneVerified;
    String email;
    boolean emailVerified;
    String firstName;
    String lastName;
    String displayName;
    String status;
    List<String> roles;
    Instant createdAt;
    Instant lastLoginAt;
}
