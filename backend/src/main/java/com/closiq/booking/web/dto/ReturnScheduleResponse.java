package com.closiq.booking.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class ReturnScheduleResponse {

    String status;
    UUID shipmentId;
    String returnReference;
    LocalDate pickupDate;
    String pickupWindow;
    Instant pickupScheduledAt;
    boolean alreadyScheduled;
}
