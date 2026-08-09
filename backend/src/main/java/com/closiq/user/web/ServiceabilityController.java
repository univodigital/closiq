package com.closiq.user.web;

import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.user.service.UserSettingsService;
import com.closiq.user.web.dto.PincodeServiceabilityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/serviceability")
@RequiredArgsConstructor
@Tag(name = "Serviceability", description = "Delivery pincode checks")
public class ServiceabilityController {

    private final UserSettingsService userSettingsService;

    @GetMapping("/pincodes/{pincode}")
    @Operation(summary = "Check if pincode is serviceable for delivery")
    public ResponseEntity<ApiResponse<PincodeServiceabilityResponse>> checkPincode(
            @PathVariable String pincode,
            HttpServletRequest request) {

        PincodeServiceabilityResponse response = userSettingsService.checkPincode(pincode);
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }
}
