package com.closiq.admin.web;

import com.closiq.admin.service.AdminProductService;
import com.closiq.admin.web.dto.AdminProductListItemResponse;
import com.closiq.admin.web.dto.UpdateAdminProductRequest;
import com.closiq.common.security.RequiresAdmin;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.common.web.ClosiqRequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@RequiresAdmin
@Tag(name = "Admin Products", description = "Product moderation")
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    @Operation(summary = "List all products")
    public ResponseEntity<ApiResponse<List<AdminProductListItemResponse>>> listProducts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {

        PagedResult<AdminProductListItemResponse> page = adminProductService.listProducts(status, pageToken, limit);
        return ResponseEntity.ok(ApiResponse.ok(
                page.getItems(),
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(page)));
    }

    @PatchMapping("/{productId}")
    @Operation(summary = "Update product status")
    public ResponseEntity<ApiResponse<AdminProductListItemResponse>> updateProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateAdminProductRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                adminProductService.updateProduct(productId, body, principal.userId()),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Archive product")
    public ResponseEntity<Void> deleteProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId) {

        adminProductService.deleteProduct(productId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
