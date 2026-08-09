package com.closiq.user.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AddressResponse {

    String id;
    String label;
    String line1;
    String line2;
    String city;
    String state;
    String pincode;
    String phone;
    boolean isDefault;
    boolean serviceable;
}
