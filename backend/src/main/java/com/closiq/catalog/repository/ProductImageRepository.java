package com.closiq.catalog.repository;

import com.closiq.catalog.domain.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(UUID productId);

    Optional<ProductImage> findByIdAndProduct_Id(UUID id, UUID productId);

    long countByProductId(UUID productId);

    List<ProductImage> findByProductIdInOrderBySortOrderAsc(Collection<UUID> productIds);
}
