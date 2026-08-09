package com.closiq.catalog.service;

import com.closiq.catalog.domain.Category;
import com.closiq.catalog.repository.CategoryRepository;
import com.closiq.catalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCatalogService productCatalogService;

    @Mock
    private com.closiq.catalog.mapper.ProductMapper productMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void listCategories_returnsActiveCategoriesWithCounts() {
        UUID id = UUID.fromString("11111111-1111-7111-8111-111111111101");
        Category category = Category.builder()
                .id(id)
                .slug("wedding")
                .name("Wedding")
                .featured(true)
                .sortOrder((short) 1)
                .status("ACTIVE")
                .build();

        when(categoryRepository.findByStatusOrderBySortOrderAsc("ACTIVE")).thenReturn(List.of(category));
        when(productRepository.countByCategoryIdAndDeletedAtIsNullAndStatus(id, "ACTIVE")).thenReturn(3L);
        when(productMapper.toCategory(category, 3L)).thenReturn(
                com.closiq.catalog.web.dto.CategoryResponse.builder()
                        .id(id.toString())
                        .slug("wedding")
                        .name("Wedding")
                        .productCount(3)
                        .featured(true)
                        .sortOrder(1)
                        .build());

        var result = categoryService.listCategories(null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getSlug()).isEqualTo("wedding");
        assertThat(result.getFirst().getProductCount()).isEqualTo(3);
    }
}
