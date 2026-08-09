package com.closiq.booking.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class TrialRejectRequest {

    @NotNull
    String reason;

    String comment;
}
