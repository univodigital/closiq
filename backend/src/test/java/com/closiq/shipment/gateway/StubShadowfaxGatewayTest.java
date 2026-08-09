package com.closiq.shipment.gateway;

import com.closiq.config.ClosiqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubShadowfaxGatewayTest {

    private StubShadowfaxGateway gateway;

    @BeforeEach
    void setUp() {
        ClosiqProperties properties = new ClosiqProperties();
        properties.getShadowfax().setWebhookSecret("test_webhook_secret");
        gateway = new StubShadowfaxGateway(properties);
    }

    @Test
    void createShipment_returnsTrackingDetails() {
        CreateShipmentResult result = gateway.createShipment(CreateShipmentCommand.builder()
                .shipmentType("OUTBOUND")
                .rentalNumber("VST-RNT-20260805-0001")
                .bookingNumber("VST-RNT-20260805-0001")
                .build());

        assertThat(result.getProviderShipmentId()).startsWith("sfx_ord_");
        assertThat(result.getTrackingNumber()).startsWith("SFX");
        assertThat(result.getPickupScheduledAt()).isNotNull();
        assertThat(result.getEstimatedDeliveryAt()).isAfter(result.getPickupScheduledAt());
    }

    @Test
    void verifyWebhookSignature_acceptsValidHmac() {
        String body = "{\"eventId\":\"evt_1\",\"status\":\"DELIVERED\"}";
        String signature = gateway.generateStubWebhookSignature(body);

        assertThat(gateway.verifyWebhookSignature(body, signature)).isTrue();
    }

    @Test
    void verifyWebhookSignature_rejectsInvalidSignature() {
        assertThat(gateway.verifyWebhookSignature("{}", "bad")).isFalse();
    }
}
