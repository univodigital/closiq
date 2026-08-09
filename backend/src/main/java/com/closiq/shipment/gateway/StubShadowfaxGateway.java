package com.closiq.shipment.gateway;

import com.closiq.common.util.IdGenerator;
import com.closiq.config.ClosiqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class StubShadowfaxGateway implements LogisticsGateway {

    private final ClosiqProperties properties;

    @Override
    public CreateShipmentResult createShipment(CreateShipmentCommand command) {
        if (!properties.getShadowfax().isStubEnabled()) {
            throw new IllegalStateException("Shadowfax stub is disabled");
        }

        String suffix = IdGenerator.uuidV7().toString().replace("-", "").substring(0, 9).toUpperCase();
        String providerShipmentId = "sfx_ord_" + suffix;
        String trackingNumber = "SFX" + ThreadLocalRandom.current().nextInt(100_000_000, 999_999_999);

        Instant pickupScheduled = Instant.now().plus(4, ChronoUnit.HOURS);
        Instant estimatedDelivery = pickupScheduled.plus(24, ChronoUnit.HOURS);

        log.debug(
                "Stub Shadowfax shipment created: {} tracking={} type={} booking={}",
                providerShipmentId,
                trackingNumber,
                command.getShipmentType(),
                command.getRentalNumber());

        return CreateShipmentResult.builder()
                .providerShipmentId(providerShipmentId)
                .trackingNumber(trackingNumber)
                .pickupScheduledAt(pickupScheduled)
                .estimatedDeliveryAt(estimatedDelivery)
                .build();
    }

    @Override
    public TrackingSnapshot fetchTracking(String providerShipmentId) {
        return TrackingSnapshot.builder()
                .status("IN_TRANSIT")
                .agentName("Rajesh K.")
                .agentPhoneMasked("+9198****3210")
                .estimatedDeliveryAt(Instant.now().plus(6, ChronoUnit.HOURS))
                .build();
    }

    public boolean verifyWebhookSignature(String rawBody, String signature) {
        String expected = hmacSha256(rawBody, properties.getShadowfax().getWebhookSecret());
        return expected.equalsIgnoreCase(signature);
    }

    /** Dev helper: generate a valid stub webhook signature. */
    public String generateStubWebhookSignature(String rawBody) {
        return hmacSha256(rawBody, properties.getShadowfax().getWebhookSecret());
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute HMAC", ex);
        }
    }
}
