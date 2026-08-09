package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.inventory.service.InventoryHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingHoldExpiryService {

    private final BookingRepository bookingRepository;
    private final InventoryHoldService inventoryHoldService;
    private final BookingTimelineService timelineService;

    @Scheduled(fixedDelayString = "${closiq.booking.hold-expiry-poll-ms:60000}")
    @Transactional
    public void releaseExpiredHolds() {
        Instant now = Instant.now();
        for (Booking booking : bookingRepository.findExpiredPendingPayment(now)) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(now);
            booking.setCancelReason("HOLD_EXPIRED");
            bookingRepository.save(booking);
            inventoryHoldService.releaseByBookingId(booking.getId(), InventoryHoldService.EXPIRED);
            timelineService.append(
                    booking.getId(),
                    null,
                    BookingStatus.CANCELLED,
                    "Hold expired — booking cancelled");
            log.info("Released expired booking hold {}", booking.getRentalNumber());
        }
    }
}
