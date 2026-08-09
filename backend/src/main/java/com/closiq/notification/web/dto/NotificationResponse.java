package com.closiq.notification.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class NotificationResponse {

    UUID id;
    String type;
    String title;
    String body;
    boolean read;
    String deepLink;
    Map<String, Object> metadata;
    Instant createdAt;
}
