package com.closiq.notification.domain;

public final class NotificationType {

    public static final String TRIAL_READY = "TRIAL_READY";
    public static final String TRIAL_ACCEPTED = "TRIAL_ACCEPTED";
    public static final String TRIAL_REJECTED = "TRIAL_REJECTED";
    public static final String OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
    public static final String BOOKING_CONFIRMED = "BOOKING_CONFIRMED";
    public static final String RETURN_SCHEDULED = "RETURN_SCHEDULED";
    public static final String RETURN_REMINDER = "RETURN_REMINDER";
    public static final String DEPOSIT_REFUNDED = "DEPOSIT_REFUNDED";
    public static final String SELLER_NEW_BOOKING = "SELLER_NEW_BOOKING";
    public static final String SELLER_PAYOUT = "SELLER_PAYOUT";
    public static final String PROMOTION = "PROMOTION";

    private NotificationType() {
    }
}
