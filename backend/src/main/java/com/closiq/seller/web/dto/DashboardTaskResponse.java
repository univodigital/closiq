package com.closiq.seller.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class DashboardTaskResponse {

    String type;
    String bookingId;
    Instant dueBy;
}
