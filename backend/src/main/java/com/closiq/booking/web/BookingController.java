package com.closiq.booking.web;

import com.closiq.booking.service.BookingLifecycleService;
import com.closiq.booking.service.BookingService;
import com.closiq.booking.web.dto.BookingDetailResponse;
import com.closiq.booking.web.dto.BookingSummaryResponse;
import com.closiq.booking.web.dto.CancelBookingRequest;
import com.closiq.booking.web.dto.CreateBookingRequest;
import com.closiq.booking.web.dto.CreateBookingResponse;
import com.closiq.booking.web.dto.ReturnRequestRequest;
import com.closiq.booking.web.dto.TimelineEventResponse;
import com.closiq.booking.web.dto.TrialRejectRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Rental booking holds and order lifecycle")
public class BookingController {

    private final BookingService bookingService;
    private final BookingLifecycleService lifecycleService;

    @PostMapping
    @Operation(summary = "Create booking hold and checkout session")
    public ResponseEntity<ApiResponse<CreateBookingResponse>> createBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest body,
            HttpServletRequest request) {

        CreateBookingResponse response = bookingService.createHold(principal.userId(), idempotencyKey, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                response, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping
    @Operation(summary = "Customer booking history")
    public ResponseEntity<ApiResponse<List<BookingSummaryResponse>>> listBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {

        PagedResult<BookingSummaryResponse> page =
                bookingService.listBookings(principal.userId(), status, pageToken, limit);

        return ResponseEntity.ok(ApiResponse.ok(
                page.getItems(),
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(page)));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking details")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.getBooking(principal.userId(), bookingId),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel booking")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> cancelBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            @Valid @RequestBody CancelBookingRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.cancelBooking(principal.userId(), bookingId, body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/{bookingId}/timeline")
    @Operation(summary = "Booking timeline events")
    public ResponseEntity<ApiResponse<List<TimelineEventResponse>>> timeline(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.getTimeline(principal.userId(), bookingId),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/{bookingId}/trial/accept")
    @Operation(summary = "Accept item during home trial")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptTrial(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                lifecycleService.acceptTrial(principal.userId(), bookingId),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/{bookingId}/trial/reject")
    @Operation(summary = "Reject item during home trial")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectTrial(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            @Valid @RequestBody TrialRejectRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                lifecycleService.rejectTrial(principal.userId(), bookingId, body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/{bookingId}/return-request")
    @Operation(summary = "Schedule return pickup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> returnRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bookingId,
            @Valid @RequestBody ReturnRequestRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                lifecycleService.requestReturn(principal.userId(), bookingId, body),
                ClosiqRequestIdFilter.getRequestId(request)));
    }
}
