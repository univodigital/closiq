package com.closiq.shipment.service;

import com.closiq.booking.domain.Booking;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Determines the next operational return pickup slot — client must not choose dates.
 */
@Service
public class ReturnPickupScheduleService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private static final List<PickupSlot> SLOTS = List.of(
            new PickupSlot("10:00-14:00", LocalTime.of(10, 0)),
            new PickupSlot("14:00-18:00", LocalTime.of(14, 0)),
            new PickupSlot("18:00-21:00", LocalTime.of(18, 0)));

    public ScheduledReturnPickup resolve(Booking booking) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate earliest = booking.getRentalEndDate().isAfter(today) ? booking.getRentalEndDate() : today;

        int slotIndex = slotIndexForNow(Instant.now());
        LocalDate pickupDate = earliest;

        if (pickupDate.equals(today) && slotIndex >= SLOTS.size()) {
            pickupDate = today.plusDays(1);
            slotIndex = 0;
        }

        PickupSlot slot = SLOTS.get(Math.min(slotIndex, SLOTS.size() - 1));
        Instant scheduledAt = pickupDate.atTime(slot.start()).atZone(ZONE).toInstant();

        return new ScheduledReturnPickup(pickupDate, slot.label(), scheduledAt);
    }

    private int slotIndexForNow(Instant now) {
        LocalTime time = now.atZone(ZONE).toLocalTime();
        for (int i = 0; i < SLOTS.size(); i++) {
            if (time.isBefore(SLOTS.get(i).start())) {
                return i;
            }
        }
        return SLOTS.size();
    }

    private record PickupSlot(String label, LocalTime start) {}

    @Value
    public static class ScheduledReturnPickup {
        LocalDate pickupDate;
        String pickupWindow;
        Instant pickupScheduledAt;
    }
}
