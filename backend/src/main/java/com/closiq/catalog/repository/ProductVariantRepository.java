package com.closiq.catalog.repository;

import com.closiq.catalog.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductIdOrderBySortOrderAsc(UUID productId);

    Optional<ProductVariant> findByIdAndProductId(UUID id, UUID productId);
}
