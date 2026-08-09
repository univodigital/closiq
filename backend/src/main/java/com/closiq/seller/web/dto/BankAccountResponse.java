package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BankAccountResponse {

    String id;
    String label;
    boolean isDefault;
    String status;
}
