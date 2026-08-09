package com.closiq.user.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccountSettingsResponse {

    String language;
    boolean marketingOptIn;
}
