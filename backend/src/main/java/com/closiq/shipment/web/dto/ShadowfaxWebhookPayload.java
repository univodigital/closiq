package com.closiq.shipment.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShadowfaxWebhookPayload {

    private String eventId;
    private String shipmentId;
    private String trackingNumber;
    private String status;
    private String label;
    private String location;
    private String occurredAt;
    private String agentName;
    private String agentPhoneMasked;
    private Map<String, Object> metadata;
}
