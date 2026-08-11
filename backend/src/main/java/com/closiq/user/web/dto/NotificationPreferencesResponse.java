package com.closiq.user.web.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationPreferencesResponse {

    boolean emailEnabled;
    boolean smsEnabled;
    boolean pushEnabled;
    boolean orderUpdates;
    boolean returnReminders;
    boolean promotions;
    boolean sellerBookingAlerts;
    boolean emailAvailable;
    boolean smsAvailable;
    boolean pushAvailable;
}
