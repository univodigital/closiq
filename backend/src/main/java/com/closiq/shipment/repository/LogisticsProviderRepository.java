package com.closiq.shipment.repository;

import com.closiq.shipment.domain.LogisticsProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LogisticsProviderRepository extends JpaRepository<LogisticsProvider, UUID> {

    Optional<LogisticsProvider> findByCode(String code);
}
