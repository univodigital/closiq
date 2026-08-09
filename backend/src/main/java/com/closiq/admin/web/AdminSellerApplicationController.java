package com.closiq.admin.web;

import com.closiq.admin.service.AdminSellerApplicationService;
import com.closiq.admin.web.dto.AdminSellerApplicationListItemResponse;
import com.closiq.admin.web.dto.RejectSellerApplicationRequest;
import com.closiq.common.security.RequiresAdmin;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/seller-applications")
@RequiredArgsConstructor
@RequiresAdmin
@Tag(name = "Admin Seller Applications", description = "Seller verification queue")
public class AdminSellerApplicationController {

    private final AdminSellerApplicationService adminSellerApplicationService;

    @GetMapping
    @Operation(summary = "List seller applications")
    public ResponseEntity<ApiResponse<List<AdminSellerApplicationListItemResponse>>> listApplications(
            @RequestParam(required = false) String status,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                adminSellerApplicationService.listApplications(status),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/{applicationId}/approve")
    @Operation(summary = "Approve seller application")
    public ResponseEntity<ApiResponse<AdminSellerApplicationListItemResponse>> approve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID applicationId,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                adminSellerApplicationService.approveApplication(applicationId, principal.userId()),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/{applicationId}/reject")
    @Operation(summary = "Reject seller application")
    public ResponseEntity<ApiResponse<AdminSellerApplicationListItemResponse>> reject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID applicationId,
            @Valid @RequestBody RejectSellerApplicationRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                adminSellerApplicationService.rejectApplication(applicationId, body, principal.userId()),
                ClosiqRequestIdFilter.getRequestId(request)));
    }
}
