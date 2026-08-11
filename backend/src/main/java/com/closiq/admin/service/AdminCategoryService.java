package com.closiq.admin.service;

import com.closiq.admin.web.dto.AdminCategoryResponse;
import com.closiq.admin.web.dto.CreateAdminCategoryRequest;
import com.closiq.admin.web.dto.UpdateAdminCategoryRequest;
import com.closiq.catalog.domain.Category;
import com.closiq.catalog.repository.CategoryRepository;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.catalog.service.ProductSpecifications;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.util.IdGenerator;
import com.closiq.common.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private static final String ACTIVE = ProductSpecifications.ACTIVE;
    private static final String DEPRECATED = "DEPRECATED";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<AdminCategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminCategoryResponse createCategory(CreateAdminCategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "A category with this name already exists");
        }

        String slug = uniqueSlug(name);
        Instant now = Instant.now();
        Category category = Category.builder()
                .id(IdGenerator.uuidV7())
                .slug(slug)
                .name(name)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .verticalCode("CLOTHING")
                .depth((short) 0)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : nextSortOrder())
                .featured(Boolean.TRUE.equals(request.getFeatured()))
                .status(ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    public AdminCategoryResponse updateCategory(UUID categoryId, UpdateAdminCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Category not found"));

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, categoryId)) {
                throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "A category with this name already exists");
            }
            category.setName(name);
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }
        if (request.getFeatured() != null) {
            category.setFeatured(request.getFeatured());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        category.setUpdatedAt(Instant.now());
        categoryRepository.save(category);
        return toResponse(category);
    }

    private AdminCategoryResponse toResponse(Category category) {
        long productCount = productRepository.countByCategoryIdAndDeletedAtIsNull(category.getId());
        return AdminCategoryResponse.builder()
                .id(category.getId().toString())
                .slug(category.getSlug())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .status(category.getStatus())
                .featured(category.isFeatured())
                .sortOrder(category.getSortOrder())
                .productCount(productCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private String uniqueSlug(String name) {
        String base = SlugUtils.slugify(name);
        if (base.isBlank()) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Category name must contain letters or numbers");
        }
        String candidate = base;
        int suffix = 2;
        while (categoryRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private short nextSortOrder() {
        return (short) (categoryRepository.findAllByOrderBySortOrderAsc().stream()
                        .mapToInt(Category::getSortOrder)
                        .max()
                        .orElse(0)
                + 1);
    }
}
