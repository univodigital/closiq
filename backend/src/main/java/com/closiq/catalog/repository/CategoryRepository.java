package com.closiq.catalog.repository;

import com.closiq.catalog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByStatusOrderBySortOrderAsc(String status);

    List<Category> findByStatusAndFeaturedTrueOrderBySortOrderAsc(String status);

    Optional<Category> findBySlugAndStatus(String slug, String status);

    Optional<Category> findByIdAndStatus(UUID id, String status);
}
