package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.domain.BookingTimeline;
import com.closiq.booking.web.dto.TimelineEventResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookingLifecycleTimelineBuilderTest {

    private final BookingLifecycleTimelineBuilder builder = new BookingLifecycleTimelineBuilder();

    @Test
    void buildsFullPipelineWithCurrentStep() {
        Booking booking = Booking.builder()
                .status(BookingStatus.OUT_FOR_DELIVERY)
                .includesTrial(true)
                .rentalEndDate(LocalDate.of(2026, 8, 18))
                .confirmedAt(Instant.parse("2026-08-10T05:00:00Z"))
                .build();

        List<BookingTimeline> history = List.of(
                timeline(BookingStatus.CONFIRMED, Instant.parse("2026-08-10T05:00:00Z")),
                timeline(BookingStatus.SELLER_ACCEPTED, Instant.parse("2026-08-10T06:00:00Z")),
                timeline(BookingStatus.OUT_FOR_DELIVERY, Instant.parse("2026-08-11T04:00:00Z")));

        List<TimelineEventResponse> events = builder.build(booking, history);

        assertThat(events).isNotEmpty();
        assertThat(events.stream().filter(e -> Boolean.TRUE.equals(e.getCompleted())).count()).isGreaterThan(0);
        assertThat(events.stream().filter(e -> Boolean.TRUE.equals(e.getCurrent())).map(TimelineEventResponse::getStatus))
                .contains(BookingStatus.OUT_FOR_DELIVERY);
        assertThat(events.stream().filter(e -> Boolean.TRUE.equals(e.getPending())).count()).isGreaterThan(0);
    }

    @Test
    void completedBookingMarksAllStepsCompleted() {
        Booking booking = Booking.builder()
                .status(BookingStatus.COMPLETED)
                .includesTrial(true)
                .rentalEndDate(LocalDate.of(2026, 8, 18))
                .completedAt(Instant.parse("2026-08-20T10:00:00Z"))
                .build();

        List<TimelineEventResponse> events = builder.build(booking, List.of());

        assertThat(events.stream().allMatch(e -> Boolean.TRUE.equals(e.getCompleted()))).isTrue();
        assertThat(events.stream().noneMatch(e -> Boolean.TRUE.equals(e.getCurrent()))).isTrue();
    }

    private BookingTimeline timeline(String status, Instant at) {
        return BookingTimeline.builder()
                .status(status)
                .label(status)
                .occurredAt(at)
                .build();
    }
}
