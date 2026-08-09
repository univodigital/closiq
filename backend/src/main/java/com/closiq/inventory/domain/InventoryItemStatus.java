package com.closiq.inventory.domain;

public final class InventoryItemStatus {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String RESERVED = "RESERVED";
    public static final String RENTED = "RENTED";
    public static final String IN_TRANSIT = "IN_TRANSIT";
    public static final String MAINTENANCE = "MAINTENANCE";
    public static final String RETIRED = "RETIRED";

    private InventoryItemStatus() {
    }
}
