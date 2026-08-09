package com.closiq.admin.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class AdminUserListItemResponse {

    String id;
    String userCode;
    String phone;
    String email;
    String firstName;
    String lastName;
    String displayName;
    String status;
    List<String> roles;
    Instant createdAt;
}
