package com.closiq.catalog.service;

import com.closiq.catalog.mapper.ProductMapper;
import com.closiq.catalog.repository.CategoryRepository;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.catalog.web.dto.CategoryResponse;
import com.closiq.catalog.web.dto.ProductSummaryResponse;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.web.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final String ACTIVE = ProductSpecifications.ACTIVE;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductCatalogService productCatalogService;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(Boolean featured) {
        var categories = Boolean.TRUE.equals(featured)
                ? categoryRepository.findByStatusAndFeaturedTrueOrderBySortOrderAsc(ACTIVE)
                : categoryRepository.findByStatusOrderBySortOrderAsc(ACTIVE);

        return categories.stream()
                .map(cat -> productMapper.toCategory(
                        cat, productRepository.countByCategoryIdAndDeletedAtIsNullAndStatus(cat.getId(), ACTIVE)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResult<ProductSummaryResponse> listCategoryProducts(
            String slug,
            String occasion,
            String size,
            Long minPrice,
            Long maxPrice,
            String city,
            Boolean featured,
            Boolean trending,
            String sort,
            String pageToken,
            Integer limit) {

        var category = categoryRepository.findBySlugAndStatus(slug, ACTIVE)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Category not found"));

        return productCatalogService.listProducts(
                occasion, category.getId(), size, minPrice, maxPrice, city, featured, trending,
                null, null, sort, pageToken, limit);
    }
}
