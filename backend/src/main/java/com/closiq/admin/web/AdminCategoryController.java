package com.closiq.admin.web;

import com.closiq.admin.service.AdminCategoryService;
import com.closiq.admin.web.dto.AdminCategoryResponse;
import com.closiq.admin.web.dto.CreateAdminCategoryRequest;
import com.closiq.admin.web.dto.UpdateAdminCategoryRequest;
import com.closiq.common.security.RequiresAdmin;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@RequiresAdmin
@Tag(name = "Admin Categories", description = "Category management")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    @Operation(summary = "List all categories")
    public ResponseEntity<ApiResponse<List<AdminCategoryResponse>>> listCategories(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminCategoryService.listCategories(), ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping
    @Operation(summary = "Create category")
    public ResponseEntity<ApiResponse<AdminCategoryResponse>> createCategory(
            @Valid @RequestBody CreateAdminCategoryRequest body,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                adminCategoryService.createCategory(body), ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PatchMapping("/{categoryId}")
    @Operation(summary = "Update category")
    public ResponseEntity<ApiResponse<AdminCategoryResponse>> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateAdminCategoryRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                adminCategoryService.updateCategory(categoryId, body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }
}
