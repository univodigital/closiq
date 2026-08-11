package com.closiq.notification.service;

import com.closiq.config.ClosiqProperties;
import com.closiq.notification.domain.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationChannelResolver {

    private final ClosiqProperties closiqProperties;

    public boolean isAvailable(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> true;
            case SMS -> closiqProperties.getNotification().isSmsEnabled();
            case PUSH -> closiqProperties.getNotification().isPushEnabled();
        };
    }
}
