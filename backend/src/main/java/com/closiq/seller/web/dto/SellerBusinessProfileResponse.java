package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SellerBusinessProfileResponse {

    String sellerId;
    String businessName;
    String verificationStatus;
    String city;
    Double rating;
    long listingCount;
}
