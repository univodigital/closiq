package com.closiq.shipment.repository;

import com.closiq.shipment.domain.LogisticsWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogisticsWebhookEventRepository extends JpaRepository<LogisticsWebhookEvent, Long> {

    boolean existsByProviderCodeAndProviderEventId(String providerCode, String providerEventId);
}
