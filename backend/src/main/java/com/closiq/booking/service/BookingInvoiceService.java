package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingItem;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.identity.repository.UserRepository;
import com.closiq.payment.domain.Payment;
import com.closiq.payment.domain.PaymentStatus;
import com.closiq.payment.repository.PaymentRepository;
import com.closiq.user.domain.Address;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingInvoiceService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SellerProfileRepository sellerProfileRepository;

    @Transactional(readOnly = true)
    public String generateHtmlInvoice(Booking booking, BookingItem item, Address address) {
        Payment payment = paymentRepository
                .findByBookingIdAndStatus(booking.getId(), PaymentStatus.CAPTURED)
                .orElse(null);

        User customer = userRepository.findById(booking.getCustomerId()).orElse(null);
        UserProfile customerProfile = userProfileRepository.findByUserId(booking.getCustomerId()).orElse(null);
        SellerProfile seller = booking.getSellerProfileId() != null
                ? sellerProfileRepository.findById(booking.getSellerProfileId()).orElse(null)
                : null;

        Map<String, Object> snapshot = item.getPriceSnapshot();
        String productTitle = snapshot.get("productTitle") != null
                ? snapshot.get("productTitle").toString()
                : "Rental item";

        String customerName = customerProfile != null && customerProfile.getDisplayName() != null
                ? escape(customerProfile.getDisplayName())
                : "Customer";
        String customerEmail = customer != null && customer.getEmail() != null
                ? escape(customer.getEmail())
                : "";
        String sellerName = seller != null ? escape(seller.getBusinessName()) : "Closiq Partner";

        String paymentStatus = payment != null ? payment.getStatus() : "PENDING";
        String paymentRef = payment != null && payment.getProviderPaymentId() != null
                ? escape(payment.getProviderPaymentId())
                : "—";

        String addressBlock = formatAddress(address);

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <title>Invoice %s</title>
                  <style>
                    body { font-family: system-ui, sans-serif; color: #111; max-width: 720px; margin: 2rem auto; }
                    h1 { font-size: 1.5rem; margin-bottom: 0.25rem; }
                    .muted { color: #666; font-size: 0.875rem; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 1.5rem; }
                    th, td { text-align: left; padding: 0.5rem 0; border-bottom: 1px solid #eee; }
                    .total { font-weight: 700; }
                    .section { margin-top: 1.5rem; }
                  </style>
                </head>
                <body>
                  <h1>Tax Invoice</h1>
                  <p class="muted">Order %s · Rental %s</p>
                  <p class="muted">Invoice date: %s</p>

                  <div class="section">
                    <strong>Bill to</strong><br/>
                    %s<br/>
                    %s<br/>
                    %s
                  </div>

                  <div class="section">
                    <strong>Seller</strong><br/>
                    %s
                  </div>

                  <table>
                    <thead>
                      <tr><th>Description</th><th>Amount (INR)</th></tr>
                    </thead>
                    <tbody>
                      <tr><td>%s — %s days rental (%s to %s)</td><td>%s</td></tr>
                      <tr><td>Security deposit</td><td>%s</td></tr>
                      <tr><td>Delivery fee</td><td>%s</td></tr>
                      <tr><td>Discount</td><td>- %s</td></tr>
                      <tr class="total"><td>Total</td><td>%s</td></tr>
                    </tbody>
                  </table>

                  <div class="section">
                    <strong>Payment</strong><br/>
                    Status: %s<br/>
                    Reference: %s<br/>
                    Method: %s
                  </div>

                  <div class="section muted">
                    Booking status: %s<br/>
                    This document confirms the commercial transaction. Deposit is held separately per rental policy.
                  </div>
                </body>
                </html>
                """.formatted(
                escape(booking.getOrderNumber()),
                escape(booking.getOrderNumber()),
                escape(booking.getRentalNumber()),
                formatInstant(booking.getConfirmedAt() != null ? booking.getConfirmedAt() : booking.getCreatedAt()),
                customerName,
                customerEmail,
                addressBlock,
                sellerName,
                escape(productTitle),
                booking.getRentalDays(),
                booking.getRentalStartDate(),
                booking.getRentalEndDate(),
                formatRupees(booking.getRentalAmount()),
                formatRupees(booking.getDepositAmount()),
                formatRupees(booking.getDeliveryFee()),
                formatRupees(booking.getDiscountAmount()),
                formatRupees(booking.getTotalAmount()),
                paymentStatus,
                paymentRef,
                payment != null && payment.getPaymentMethod() != null ? escape(payment.getPaymentMethod()) : "—",
                booking.getStatus());
    }

    public boolean isInvoiceAvailable(Booking booking) {
        return !BookingStatus.PENDING_PAYMENT.equals(booking.getStatus());
    }

    private String formatAddress(Address address) {
        if (address == null) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(escape(address.getLine1()));
        if (address.getLine2() != null && !address.getLine2().isBlank()) {
            sb.append(", ").append(escape(address.getLine2()));
        }
        sb.append("<br/>")
                .append(escape(address.getCity()))
                .append(", ")
                .append(escape(address.getState()))
                .append(" ")
                .append(escape(address.getPincode()));
        return sb.toString();
    }

    private String formatRupees(long rupees) {
        return String.format(Locale.ENGLISH, "₹%,d", rupees);
    }

    private String formatInstant(java.time.Instant instant) {
        return DATE_FMT.format(instant.atZone(ZONE));
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
