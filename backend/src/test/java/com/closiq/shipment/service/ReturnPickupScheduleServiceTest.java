package com.closiq.shipment.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnPickupScheduleServiceTest {

    private ReturnPickupScheduleService service;

    @BeforeEach
    void setUp() {
        service = new ReturnPickupScheduleService();
    }

    @Test
    void resolve_usesRentalEndDateWhenInFuture() {
        LocalDate futureEnd = LocalDate.now().plusDays(5);
        Booking booking = Booking.builder()
                .status(BookingStatus.RENTAL_ACTIVE)
                .rentalEndDate(futureEnd)
                .build();

        var slot = service.resolve(booking);

        assertThat(slot.getPickupDate()).isEqualTo(futureEnd);
        assertThat(slot.getPickupWindow()).isNotBlank();
        assertThat(slot.getPickupScheduledAt()).isNotNull();
    }

    @Test
    void resolve_neverUsesPastDateBeforeToday() {
        Booking booking = Booking.builder()
                .status(BookingStatus.RENTAL_ACTIVE)
                .rentalEndDate(LocalDate.now().minusDays(3))
                .build();

        var slot = service.resolve(booking);

        assertThat(slot.getPickupDate()).isAfterOrEqualTo(LocalDate.now());
    }
}
