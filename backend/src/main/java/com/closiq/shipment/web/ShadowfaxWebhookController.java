package com.closiq.shipment.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.closiq.shipment.service.ShipmentWebhookService;
import com.closiq.shipment.web.dto.ShadowfaxWebhookPayload;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Hidden
public class ShadowfaxWebhookController {

    private final ShipmentWebhookService webhookService;
    private final ObjectMapper objectMapper;

    @PostMapping("/shadowfax")
    public ResponseEntity<Map<String, Boolean>> handleShadowfaxWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Shadowfax-Signature", required = false) String signature)
            throws com.fasterxml.jackson.core.JsonProcessingException {

        ShadowfaxWebhookPayload payload = objectMapper.readValue(rawBody, ShadowfaxWebhookPayload.class);
        webhookService.processShadowfaxWebhook(rawBody, signature != null ? signature : "", payload);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
