package com.closiq.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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
import java.util.UUID;

@Entity
@Table(name = "booking_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingItem {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(name = "inventory_item_id", nullable = false)
    private UUID inventoryItemId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "price_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> priceSnapshot;

    @Column(nullable = false)
    private short quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
