package com.closiq.shipment.repository;

import com.closiq.shipment.domain.ShipmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {

    List<ShipmentEvent> findByShipmentIdOrderByOccurredAtAsc(UUID shipmentId);

    boolean existsByShipmentIdAndProviderEventId(UUID shipmentId, String providerEventId);
}
