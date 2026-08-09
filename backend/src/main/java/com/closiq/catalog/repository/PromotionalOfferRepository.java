package com.closiq.catalog.repository;

import com.closiq.catalog.domain.PromotionalOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PromotionalOfferRepository extends JpaRepository<PromotionalOffer, UUID> {

    List<PromotionalOffer> findByStatusAndValidUntilAfterOrderByValidUntilAsc(String status, Instant now);
}
