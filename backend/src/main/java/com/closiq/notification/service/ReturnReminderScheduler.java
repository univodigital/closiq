package com.closiq.notification.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.config.ClosiqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnReminderScheduler {

    private final BookingRepository bookingRepository;
    private final NotificationDispatchService notificationDispatchService;
    private final ClosiqProperties closiqProperties;

    @Scheduled(fixedDelayString = "${closiq.notification.return-reminder-poll-ms:3600000}")
    @Transactional
    public void sendDueReminders() {
        int hoursBeforeEnd = closiqProperties.getNotification().getReturnReminderHoursBeforeEnd();
        int daysBeforeEnd = Math.max(1, (hoursBeforeEnd + 23) / 24);
        LocalDate targetEndDate = LocalDate.now().plusDays(daysBeforeEnd);

        for (Booking booking : bookingRepository.findActiveRentalsEndingOn(targetEndDate)) {
            notificationDispatchService.returnReminder(booking);
            log.info("Return reminder dispatched for booking {}", booking.getRentalNumber());
        }
    }
}
