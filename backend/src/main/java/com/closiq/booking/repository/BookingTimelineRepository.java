package com.closiq.booking.repository;

import com.closiq.booking.domain.BookingTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingTimelineRepository extends JpaRepository<BookingTimeline, Long> {

    List<BookingTimeline> findByBookingIdOrderByOccurredAtAsc(UUID bookingId);
}
