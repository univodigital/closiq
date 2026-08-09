package com.closiq.shipment.gateway;

public interface LogisticsGateway {

    CreateShipmentResult createShipment(CreateShipmentCommand command);

    TrackingSnapshot fetchTracking(String providerShipmentId);
}
