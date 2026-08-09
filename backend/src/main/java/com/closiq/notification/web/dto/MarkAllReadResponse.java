package com.closiq.notification.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarkAllReadResponse {

    int markedCount;
}
