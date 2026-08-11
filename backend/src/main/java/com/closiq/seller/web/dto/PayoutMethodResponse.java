package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PayoutMethodResponse {

    String id;
    String type;
    String label;
    boolean isDefault;
    boolean verified;
}
