package com.closiq.user.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PincodeServiceabilityResponse {

    String pincode;
    boolean serviceable;
    String city;
    String state;
    int estimatedDeliveryDays;
    String launchPhase;
}
