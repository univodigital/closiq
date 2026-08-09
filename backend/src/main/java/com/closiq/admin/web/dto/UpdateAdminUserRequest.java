package com.closiq.admin.web.dto;

import lombok.Value;

import java.util.List;

@Value
public class UpdateAdminUserRequest {

    String status;
    List<String> roles;
}
