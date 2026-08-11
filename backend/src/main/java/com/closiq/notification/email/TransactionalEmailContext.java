package com.closiq.notification.email;

import java.time.LocalDate;

public record TransactionalEmailContext(
        String orderLabel,
        LocalDate rentalStartDate,
        LocalDate rentalEndDate,
        long totalAmountPaise,
        long depositAmountPaise,
        String actionUrl,
        String trackingUrl) {

    public static TransactionalEmailContext forBooking(
            String orderLabel,
            LocalDate rentalStartDate,
            LocalDate rentalEndDate,
            long totalAmountPaise,
            long depositAmountPaise,
            String actionUrl) {

        return new TransactionalEmailContext(
                orderLabel,
                rentalStartDate,
                rentalEndDate,
                totalAmountPaise,
                depositAmountPaise,
                actionUrl,
                actionUrl);
    }
}
