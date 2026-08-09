package com.closiq.user.web.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserPreferencesResponse {

    String size;
    List<String> occasions;
}
