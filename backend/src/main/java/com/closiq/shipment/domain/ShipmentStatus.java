package com.closiq.shipment.domain;

public final class ShipmentStatus {

    public static final String CREATED = "CREATED";
    public static final String PICKED_UP = "PICKED_UP";
    public static final String IN_TRANSIT = "IN_TRANSIT";
    public static final String OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
    public static final String DELIVERED = "DELIVERED";
    public static final String FAILED = "FAILED";
    public static final String RETURNED_TO_SELLER = "RETURNED_TO_SELLER";

    private ShipmentStatus() {
    }
}
