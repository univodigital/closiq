package com.closiq.user.web.dto;

import lombok.Value;

@Value
public class UpdateAccountSettingsRequest {

    String language;
    Boolean marketingOptIn;
}
