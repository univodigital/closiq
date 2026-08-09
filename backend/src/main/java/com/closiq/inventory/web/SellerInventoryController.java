package com.closiq.inventory.web;

import com.closiq.common.security.RequiresSeller;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.inventory.service.SellerInventoryService;
import com.closiq.inventory.web.dto.CreateInventoryBlockRequest;
import com.closiq.inventory.web.dto.InventoryBlockResponse;
import com.closiq.inventory.web.dto.SellerInventoryResponse;
import com.closiq.inventory.web.dto.UpdateSellerInventoryRequest;
import com.closiq.seller.web.dto.SellerInventoryBlockListItemResponse;
import com.closiq.seller.service.SellerContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequiredArgsConstructor
@Tag(name = "Seller Inventory", description = "Variant stock and availability blocks")
public class SellerInventoryController {

    private final SellerInventoryService sellerInventoryService;
    private final SellerContextService sellerContextService;

    @GetMapping("/api/v1/seller/inventory/blocks")
    @RequiresSeller
    @Operation(summary = "List availability blocks for seller listings")
    public ResponseEntity<ApiResponse<List<SellerInventoryBlockListItemResponse>>> listBlocks(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        var seller = sellerContextService.requireVerifiedSeller(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(
                sellerInventoryService.listBlocks(seller),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/api/v1/seller/products/{productId}/inventory")
    @RequiresSeller
    @Operation(summary = "Get variant quantities and booking summary")
    public ResponseEntity<ApiResponse<SellerInventoryResponse>> getInventory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            HttpServletRequest request) {

        var seller = sellerContextService.requireVerifiedSeller(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(
                sellerInventoryService.getInventory(seller, productId),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PatchMapping("/api/v1/seller/products/{productId}/inventory")
    @RequiresSeller
    @Operation(summary = "Update variant stock quantities")
    public ResponseEntity<ApiResponse<SellerInventoryResponse>> updateInventory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateSellerInventoryRequest body,
            HttpServletRequest request) {

        var seller = sellerContextService.requireVerifiedSeller(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(
                sellerInventoryService.updateInventory(seller, productId, body, principal.userId()),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/api/v1/seller/inventory/blocks")
    @RequiresSeller
    @Operation(summary = "Block dates for cleaning or maintenance")
    public ResponseEntity<ApiResponse<InventoryBlockResponse>> createBlock(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateInventoryBlockRequest body,
            HttpServletRequest request) {

        var seller = sellerContextService.requireVerifiedSeller(principal.userId());
        InventoryBlockResponse response = sellerInventoryService.createBlock(seller, body, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @DeleteMapping("/api/v1/seller/inventory/blocks/{blockId}")
    @RequiresSeller
    @Operation(summary = "Remove an availability block")
    public ResponseEntity<Void> removeBlock(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID blockId) {

        var seller = sellerContextService.requireVerifiedSeller(principal.userId());
        sellerInventoryService.removeBlock(seller, principal.userId(), blockId);
        return ResponseEntity.noContent().build();
    }
}
