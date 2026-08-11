package com.closiq.seller.web;

import com.closiq.booking.domain.BookingStatus;
import com.closiq.common.security.RequiresSeller;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.seller.service.SellerBookingService;
import com.closiq.seller.web.dto.AcceptSellerBookingRequest;
import com.closiq.seller.web.dto.RejectSellerBookingRequest;
import com.closiq.seller.web.dto.SellerBookingDetailResponse;
import com.closiq.seller.web.dto.SellerBookingHistoryResponse;
import com.closiq.seller.web.dto.SellerBookingListItemResponse;
import com.closiq.seller.web.dto.SellerRejectPreviewResponse;
import com.closiq.shipment.service.ShipmentService;
import com.closiq.shipment.web.dto.ReadyForPickupRequest;
import com.closiq.seller.web.dto.ReleaseDepositRequest;
import com.closiq.shipment.web.dto.ReadyForPickupResponse;
import com.closiq.shipment.web.dto.ShipmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller/bookings")
@RequiredArgsConstructor
@RequiresSeller
@Tag(name = "Seller Bookings", description = "Seller booking management and fulfillment")
public class SellerBookingController {

    private final SellerBookingService sellerBookingService;
    private final ShipmentService shipmentService;

    @GetMapping
    @Operation(summary = "List incoming and active seller bookings")
    public ResponseEntity<ApiResponse<List<SellerBookingListItemResponse>>> listBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "rentalStart:asc") String sort,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {

        PagedResult<SellerBookingListItemResponse> page = sellerBookingService.listBookings(
                principal.userId(), status, productId, startDate, endDate, sort, pageToken, limit);

        return ResponseEntity.ok(ApiResponse.ok(
                page.getItems(),
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(page)));
    }

    @GetMapping("/history")
    @Operation(summary = "Completed and cancelled booking history")
    public ResponseEntity<ApiResponse<SellerBookingHistoryResponse>> history(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {

        List<String> statuses = status != null && !status.isBlank()
                ? Arrays.asList(status.split(","))
                : List.of();

        SellerBookingHistoryResponse response = sellerBookingService.getHistory(
                principal.userId(), page, limit, startDate, endDate, statuses);

        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Seller booking detail")
    public ResponseEntity<ApiResponse<SellerBookingDetailResponse>> getBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                sellerBookingService.getBooking(principal.userId(), bookingId),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/{bookingId}/reject-preview")
    @Operation(summary = "Preview customer refund before seller rejection")
    public ResponseEntity<ApiResponse<SellerRejectPreviewResponse>> rejectPreview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                sellerBookingService.getRejectPreview(principal.userId(), bookingId),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/{bookingId}/accept")
    @Operation(summary = "Accept incoming booking")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            @Valid @RequestBody AcceptSellerBookingRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                sellerBookingService.acceptBooking(principal.userId(), bookingId, body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/{bookingId}/reject")
    @Operation(summary = "Reject booking and initiate refund")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            @Valid @RequestBody RejectSellerBookingRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                sellerBookingService.rejectBooking(principal.userId(), bookingId, body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/{bookingId}/ready-for-pickup")
    @Operation(summary = "Mark item prepared and schedule outbound pickup")
    public ResponseEntity<ApiResponse<ReadyForPickupResponse>> readyForPickup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            @Valid @RequestBody ReadyForPickupRequest body,
            HttpServletRequest request) {

        ShipmentResponse shipment = shipmentService.markReadyForPickup(principal.userId(), bookingId, body);
        ReadyForPickupResponse response = ReadyForPickupResponse.builder()
                .status(BookingStatus.PREPARING)
                .shipmentId(shipment.getShipmentId())
                .pickupScheduledAt(shipment.getPickupScheduledAt())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/{bookingId}/release-deposit")
    @Operation(summary = "Release security deposit after return inspection")
    public ResponseEntity<ApiResponse<Map<String, Object>>> releaseDeposit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) ReleaseDepositRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                sellerBookingService.releaseDeposit(principal.userId(), bookingId, idempotencyKey, body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }
}
