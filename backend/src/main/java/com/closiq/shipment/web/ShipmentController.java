package com.closiq.shipment.web;

import com.closiq.booking.web.dto.ReturnScheduleResponse;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.shipment.service.ShipmentService;
import com.closiq.shipment.web.dto.ShipmentResponse;
import com.closiq.shipment.web.dto.ShipmentStatusResponse;
import com.closiq.shipment.web.dto.ShipmentTrackResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments", description = "Outbound and return shipment tracking")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping("/{bookingId}/track")
    @Operation(summary = "Live tracking for outbound or return shipment")
    public ResponseEntity<ApiResponse<ShipmentTrackResponse>> track(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            @RequestParam(defaultValue = "OUTBOUND") String type,
            HttpServletRequest request) {

        ShipmentTrackResponse response = shipmentService.track(principal.userId(), bookingId, type);
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/{bookingId}/status")
    @Operation(summary = "Lightweight shipment status poll")
    public ResponseEntity<ApiResponse<ShipmentStatusResponse>> status(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            @RequestParam(defaultValue = "OUTBOUND") String type,
            HttpServletRequest request) {

        ShipmentStatusResponse response = shipmentService.getStatus(principal.userId(), bookingId, type);
        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/{bookingId}/return-pickup")
    @Operation(summary = "Schedule return pickup via logistics provider (backend assigns slot)")
    public ResponseEntity<ApiResponse<ReturnScheduleResponse>> returnPickup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            @RequestBody(required = false) com.closiq.booking.web.dto.ReturnRequestRequest body,
            HttpServletRequest request) {

        com.closiq.booking.web.dto.ReturnRequestRequest safeBody =
                body != null ? body : new com.closiq.booking.web.dto.ReturnRequestRequest(null);
        ReturnScheduleResponse response = shipmentService.scheduleReturnPickup(principal.userId(), bookingId, safeBody);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }
}
