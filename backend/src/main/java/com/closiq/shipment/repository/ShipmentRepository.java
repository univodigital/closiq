package com.closiq.shipment.repository;

import com.closiq.shipment.domain.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByBookingIdAndShipmentType(UUID bookingId, String shipmentType);

    Optional<Shipment> findByProviderShipmentId(String providerShipmentId);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);
}
