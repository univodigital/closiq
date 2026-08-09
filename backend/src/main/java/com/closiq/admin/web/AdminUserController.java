package com.closiq.admin.web;

import com.closiq.admin.service.AdminUserService;
import com.closiq.admin.web.dto.AdminUserDetailResponse;
import com.closiq.admin.web.dto.AdminUserListItemResponse;
import com.closiq.admin.web.dto.CreateAdminUserRequest;
import com.closiq.admin.web.dto.UpdateAdminUserRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@RequiresAdmin
@Tag(name = "Admin Users", description = "User management")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "List users")
    public ResponseEntity<ApiResponse<List<AdminUserListItemResponse>>> listUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {

        PagedResult<AdminUserListItemResponse> page = adminUserService.listUsers(status, pageToken, limit);
        return ResponseEntity.ok(ApiResponse.ok(
                page.getItems(),
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(page)));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user detail")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUser(
            @PathVariable UUID userId,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                adminUserService.getUser(userId),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping
    @Operation(summary = "Create user")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> createUser(
            @Valid @RequestBody CreateAdminUserRequest body,
            HttpServletRequest request) {

        AdminUserDetailResponse response = adminUserService.createUser(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "Update user status or roles")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> updateUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateAdminUserRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                adminUserService.updateUser(userId, body, principal.userId()),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Soft-delete user")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId) {

        adminUserService.deleteUser(userId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
