package com.closiq.user.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeleteAccountPreviewResponse {
    long activeBookings;
    boolean canDelete;
    String message;
}
