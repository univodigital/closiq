package com.closiq.shipment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.shipment.domain.LogisticsWebhookEvent;
import com.closiq.shipment.domain.Shipment;
import com.closiq.shipment.domain.ShipmentStatus;
import com.closiq.shipment.gateway.StubShadowfaxGateway;
import com.closiq.shipment.repository.LogisticsWebhookEventRepository;
import com.closiq.shipment.repository.ShipmentEventRepository;
import com.closiq.shipment.repository.ShipmentRepository;
import com.closiq.shipment.web.dto.ShadowfaxWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentWebhookService {

    private static final String SHADOWFAX_CODE = "SHADOWFAX";

    private final StubShadowfaxGateway shadowfaxGateway;
    private final LogisticsWebhookEventRepository webhookEventRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final ShipmentService shipmentService;
    private final ShipmentBookingSyncService bookingSyncService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processShadowfaxWebhook(String rawBody, String signature, ShadowfaxWebhookPayload payload) {
        if (!shadowfaxGateway.verifyWebhookSignature(rawBody, signature)) {
            throw new ClosiqException(ErrorCode.UNAUTHORIZED, "Invalid webhook signature");
        }

        String eventId = payload.getEventId();
        if (eventId == null || eventId.isBlank()) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "eventId is required");
        }

        if (webhookEventRepository.existsByProviderCodeAndProviderEventId(SHADOWFAX_CODE, eventId)) {
            log.debug("Duplicate Shadowfax webhook event {}", eventId);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> payloadMap = objectMapper.convertValue(payload, Map.class);

        LogisticsWebhookEvent webhookEvent = webhookEventRepository.save(LogisticsWebhookEvent.builder()
                .providerCode(SHADOWFAX_CODE)
                .providerEventId(eventId)
                .payload(payloadMap)
                .receivedAt(Instant.now())
                .build());

        Shipment shipment = resolveShipment(payload);
        if (shipment == null) {
            log.warn("Shadowfax webhook for unknown shipment: {}", payload.getShipmentId());
            webhookEvent.setProcessedAt(Instant.now());
            webhookEventRepository.save(webhookEvent);
            return;
        }

        if (shipmentEventRepository.existsByShipmentIdAndProviderEventId(shipment.getId(), eventId)) {
            webhookEvent.setProcessedAt(Instant.now());
            webhookEventRepository.save(webhookEvent);
            return;
        }

        String status = normalizeStatus(payload.getStatus());
        Instant occurredAt = parseOccurredAt(payload.getOccurredAt());

        shipment.setStatus(status);
        if (ShipmentStatus.PICKED_UP.equals(status)) {
            shipment.setPickedUpAt(occurredAt);
        }
        if (ShipmentStatus.DELIVERED.equals(status)) {
            shipment.setDeliveredAt(occurredAt);
        }
        if (payload.getAgentName() != null) {
            shipment.setAgentName(payload.getAgentName());
        }
        if (payload.getAgentPhoneMasked() != null) {
            shipment.setAgentPhoneMasked(payload.getAgentPhoneMasked());
        }
        shipmentRepository.save(shipment);

        shipmentService.appendEvent(
                shipment.getId(),
                status,
                payload.getLabel() != null ? payload.getLabel() : status,
                payload.getLocation(),
                eventId,
                payloadMap);

        bookingSyncService.syncBookingStatus(shipment, status, shipment.getBookingId());

        webhookEvent.setProcessedAt(Instant.now());
        webhookEventRepository.save(webhookEvent);
    }

    private Shipment resolveShipment(ShadowfaxWebhookPayload payload) {
        if (payload.getShipmentId() != null) {
            return shipmentRepository.findByProviderShipmentId(payload.getShipmentId()).orElse(null);
        }
        if (payload.getTrackingNumber() != null) {
            return shipmentRepository.findByTrackingNumber(payload.getTrackingNumber()).orElse(null);
        }
        return null;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return ShipmentStatus.CREATED;
        }
        return status.toUpperCase();
    }

    private Instant parseOccurredAt(String occurredAt) {
        if (occurredAt == null || occurredAt.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(occurredAt);
        } catch (DateTimeParseException ex) {
            return Instant.now();
        }
    }
}
