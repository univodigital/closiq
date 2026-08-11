package com.closiq.seller.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.config.ClosiqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SellerAcceptanceServiceTest {

    private SellerAcceptanceService acceptanceService;

    @BeforeEach
    void setUp() {
        ClosiqProperties properties = new ClosiqProperties();
        properties.getBooking().setSellerAcceptSlaHours(24);
        acceptanceService = new SellerAcceptanceService(properties);
    }

    @Test
    void acceptanceExpiredAfterConfiguredSla() {
        Booking booking = Booking.builder()
                .status(BookingStatus.CONFIRMED)
                .confirmedAt(Instant.now().minus(25, ChronoUnit.HOURS))
                .build();

        assertThat(acceptanceService.isAcceptanceExpired(booking)).isTrue();
        assertThatThrownBy(() -> acceptanceService.assertAcceptanceOpen(booking))
                .isInstanceOf(ClosiqException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void acceptanceOpenWithinSla() {
        Booking booking = Booking.builder()
                .status(BookingStatus.CONFIRMED)
                .confirmedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        assertThat(acceptanceService.isAcceptanceExpired(booking)).isFalse();
        acceptanceService.assertAcceptanceOpen(booking);
    }
}
