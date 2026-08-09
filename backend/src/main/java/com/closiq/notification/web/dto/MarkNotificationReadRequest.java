package com.closiq.notification.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class MarkNotificationReadRequest {

    @NotNull
    Boolean read;
}
