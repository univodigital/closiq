package com.closiq.user.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SellerProfileResponse {

    String sellerId;
    String businessName;
    String verificationStatus;
    String city;
    Double rating;
    long listingCount;
}
