package com.closiq.identity.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SellerProfileStubResponse {

    String sellerId;
    String businessName;
    String verificationStatus;
}
