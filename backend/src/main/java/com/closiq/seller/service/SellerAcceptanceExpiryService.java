package com.closiq.seller.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.service.BookingTimelineService;
import com.closiq.inventory.service.InventoryHoldService;
import com.closiq.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerAcceptanceExpiryService {

    private final BookingRepository bookingRepository;
    private final SellerAcceptanceService acceptanceService;
    private final InventoryHoldService inventoryHoldService;
    private final RefundService refundService;
    private final BookingTimelineService timelineService;

    @Scheduled(fixedDelayString = "${closiq.booking.hold-expiry-poll-ms:60000}")
    @Transactional
    public void expireUnacceptedBookings() {
        Instant now = Instant.now();
        for (Booking booking : bookingRepository.findConfirmedPastAcceptanceDeadline(now, acceptanceService.acceptSlaHours())) {
            if (!BookingStatus.CONFIRMED.equals(booking.getStatus())) {
                continue;
            }
            if (!acceptanceService.isAcceptanceExpired(booking)) {
                continue;
            }

            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(now);
            booking.setCancelReason(SellerAcceptanceService.EXPIRED_CANCEL_REASON);
            booking.setCancelComment("Seller did not accept within the acceptance window");
            bookingRepository.save(booking);

            inventoryHoldService.releaseByBookingId(booking.getId(), InventoryHoldService.RELEASED);
            refundService.initiateBookingRefund(
                    booking,
                    RefundService.TYPE_FULL,
                    null,
                    SellerAcceptanceService.EXPIRED_CANCEL_REASON,
                    "acceptance-expired-" + booking.getId());

            timelineService.append(
                    booking.getId(),
                    null,
                    BookingStatus.CANCELLED,
                    "Acceptance window expired — booking cancelled");

            log.info("Cancelled booking {} due to acceptance SLA expiry", booking.getRentalNumber());
        }
    }
}
