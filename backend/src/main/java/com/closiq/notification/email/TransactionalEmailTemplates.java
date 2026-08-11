package com.closiq.notification.email;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class TransactionalEmailTemplates {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private TransactionalEmailTemplates() {
    }

    static String orderConfirmed(TransactionalEmailContext ctx) {
        return """
                <p>Your order has been confirmed.</p>
                <p><strong>Order:</strong> %s</p>
                <p><strong>Rental dates:</strong> %s – %s</p>
                <p><strong>Total:</strong> ₹%s</p>
                <p><strong>Deposit:</strong> ₹%s</p>
                <p><a href="%s">View order</a></p>
                """
                .formatted(
                        ctx.orderLabel(),
                        formatDate(ctx.rentalStartDate()),
                        formatDate(ctx.rentalEndDate()),
                        formatRupees(ctx.totalAmountPaise()),
                        formatRupees(ctx.depositAmountPaise()),
                        ctx.actionUrl());
    }

    static String outForDelivery(TransactionalEmailContext ctx) {
        return """
                <p>Your Closiq order %s is on the way.</p>
                <p><a href="%s">Track delivery</a></p>
                """
                .formatted(ctx.orderLabel(), ctx.trackingUrl());
    }

    static String returnReminder(TransactionalEmailContext ctx) {
        return """
                <p>Your rental for order %s ends on %s.</p>
                <p>Please prepare the item for return.</p>
                <p><a href="%s">View return details</a></p>
                """
                .formatted(ctx.orderLabel(), formatDate(ctx.rentalEndDate()), ctx.actionUrl());
    }

    private static String formatDate(LocalDate date) {
        return date != null ? DATE_FMT.format(date) : "—";
    }

    private static String formatRupees(long amountPaise) {
        return String.format(Locale.ENGLISH, "%.2f", amountPaise / 100.0);
    }
}
