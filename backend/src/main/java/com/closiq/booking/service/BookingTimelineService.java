package com.closiq.booking.service;

import com.closiq.booking.domain.BookingTimeline;
import com.closiq.booking.repository.BookingTimelineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingTimelineService {

    private final BookingTimelineRepository timelineRepository;

    @Transactional
    public void append(UUID bookingId, UUID actorId, String status, String label, String description) {
        timelineRepository.save(BookingTimeline.builder()
                .bookingId(bookingId)
                .actorId(actorId)
                .status(status)
                .label(label)
                .description(description)
                .occurredAt(Instant.now())
                .build());
    }

    @Transactional
    public void append(UUID bookingId, UUID actorId, String status, String label) {
        append(bookingId, actorId, status, label, null);
    }
}
