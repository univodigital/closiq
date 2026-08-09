package com.closiq.booking.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class TimelineEventResponse {

    String status;
    String label;
    Instant timestamp;
    Boolean completed;
    Boolean current;
    Boolean pending;
}
