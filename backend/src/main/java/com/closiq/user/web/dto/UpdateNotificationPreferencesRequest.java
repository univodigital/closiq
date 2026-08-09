package com.closiq.user.web.dto;

import lombok.Value;

@Value
public class UpdateNotificationPreferencesRequest {

    Boolean emailEnabled;
    Boolean smsEnabled;
    Boolean pushEnabled;
    Boolean orderUpdates;
    Boolean promotions;
    Boolean sellerBookingAlerts;
}
