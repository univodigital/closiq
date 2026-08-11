package com.closiq.admin.service;

import com.closiq.admin.web.dto.CreateAdminCategoryRequest;
import com.closiq.catalog.repository.CategoryRepository;
import com.closiq.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    private AdminCategoryService adminCategoryService;

    @BeforeEach
    void setUp() {
        adminCategoryService = new AdminCategoryService(categoryRepository, productRepository);
    }

    @Test
    void createCategoryGeneratesUniqueSlug() {
        when(categoryRepository.existsByNameIgnoreCase("Traditional Wear")).thenReturn(false);
        when(categoryRepository.existsBySlug("traditional-wear")).thenReturn(false);
        when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of());
        when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.countByCategoryIdAndDeletedAtIsNull(any())).thenReturn(0L);

        var response = adminCategoryService.createCategory(
                new CreateAdminCategoryRequest("Traditional Wear", "Occasion wear", null, false, null));

        assertThat(response.getSlug()).isEqualTo("traditional-wear");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(categoryRepository).save(any());
    }
}
