package com.closiq.payment.domain;

public final class PaymentStatus {

    public static final String CREATED = "CREATED";
    public static final String AUTHORIZED = "AUTHORIZED";
    public static final String CAPTURED = "CAPTURED";
    public static final String FAILED = "FAILED";
    public static final String REFUNDED = "REFUNDED";
    public static final String PARTIALLY_REFUNDED = "PARTIALLY_REFUNDED";

    private PaymentStatus() {
    }
}
