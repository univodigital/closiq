package com.closiq.shipment.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.util.UUID;

@Value
public class ReadyForPickupRequest {

    @NotNull
    UUID pickupAddressId;

    @NotNull
    String pickupTimeSlot;

    String handoffNotes;
}
