package com.closiq.catalog.repository;

import com.closiq.catalog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByStatusOrderBySortOrderAsc(String status);

    List<Category> findAllByOrderBySortOrderAsc();

    List<Category> findByStatusAndFeaturedTrueOrderBySortOrderAsc(String status);

    Optional<Category> findBySlugAndStatus(String slug, String status);

    Optional<Category> findByIdAndStatus(UUID id, String status);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND c.id <> :excludeId")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("excludeId") UUID excludeId);
}
