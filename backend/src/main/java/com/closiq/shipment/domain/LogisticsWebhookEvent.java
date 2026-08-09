package com.closiq.shipment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "logistics_webhook_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_code", nullable = false, length = 30)
    private String providerCode;

    @Column(name = "provider_event_id", nullable = false, length = 100)
    private String providerEventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
