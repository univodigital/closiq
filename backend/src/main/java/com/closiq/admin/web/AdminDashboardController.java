package com.closiq.admin.web;

import com.closiq.admin.service.AdminDashboardService;
import com.closiq.admin.web.dto.AdminDashboardResponse;
import com.closiq.common.security.RequiresAdmin;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@RequiresAdmin
@Tag(name = "Admin Dashboard", description = "Platform overview metrics")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    @Operation(summary = "Platform dashboard metrics")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> dashboard(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminDashboardService.getDashboard(),
                ClosiqRequestIdFilter.getRequestId(request)));
    }
}
