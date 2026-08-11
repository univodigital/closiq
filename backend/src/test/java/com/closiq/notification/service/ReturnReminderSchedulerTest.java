package com.closiq.notification.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.config.ClosiqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnReminderSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @InjectMocks
    private ReturnReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        ClosiqProperties properties = new ClosiqProperties();
        properties.getNotification().setReturnReminderHoursBeforeEnd(24);
        scheduler = new ReturnReminderScheduler(bookingRepository, notificationDispatchService, properties);
    }

    @Test
    void dispatchesReminderForActiveRentalsEndingTomorrow() {
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .rentalNumber("CLQ-1001")
                .status(BookingStatus.RENTAL_ACTIVE)
                .rentalEndDate(LocalDate.now().plusDays(1))
                .build();

        when(bookingRepository.findActiveRentalsEndingOn(LocalDate.now().plusDays(1)))
                .thenReturn(List.of(booking));

        scheduler.sendDueReminders();

        verify(notificationDispatchService).returnReminder(booking);
    }

    @Test
    void skipsWhenNoEligibleBookings() {
        when(bookingRepository.findActiveRentalsEndingOn(any())).thenReturn(List.of());

        scheduler.sendDueReminders();

        verify(notificationDispatchService, never()).returnReminder(any());
    }
}
