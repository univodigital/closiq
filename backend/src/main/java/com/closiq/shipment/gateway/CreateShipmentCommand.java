package com.closiq.shipment.gateway;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class CreateShipmentCommand {

    String shipmentType;
    UUID bookingId;
    String rentalNumber;
    /** @deprecated Use {@link #rentalNumber}. Kept for backward compatibility. */
    String bookingNumber;
    UUID originAddressId;
    UUID destinationAddressId;
    String pickupTimeSlot;
    String handoffNotes;
}
