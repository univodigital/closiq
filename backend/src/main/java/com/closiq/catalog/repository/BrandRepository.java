package com.closiq.catalog.repository;

import com.closiq.catalog.domain.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    List<Brand> findByIdIn(Collection<UUID> ids);

    Optional<Brand> findBySlug(String slug);
}
