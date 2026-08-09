package com.closiq.booking.domain;

public final class BookingStatus {

    public static final String PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String SELLER_ACCEPTED = "SELLER_ACCEPTED";
    public static final String PREPARING = "PREPARING";
    public static final String OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
    public static final String TRIAL_READY = "TRIAL_READY";
    public static final String TRIAL_REJECTED = "TRIAL_REJECTED";
    public static final String RENTAL_ACTIVE = "RENTAL_ACTIVE";
    public static final String RETURN_SCHEDULED = "RETURN_SCHEDULED";
    public static final String RETURN_IN_TRANSIT = "RETURN_IN_TRANSIT";
    public static final String RETURNED = "RETURNED";
    public static final String INSPECTION = "INSPECTION";
    public static final String DEPOSIT_REFUNDED = "DEPOSIT_REFUNDED";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String REFUND_PENDING = "REFUND_PENDING";

    private BookingStatus() {
    }
}
