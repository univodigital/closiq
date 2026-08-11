package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.config.ClosiqProperties;
import com.closiq.notification.service.NotificationDispatchService;
import com.closiq.payment.domain.Payment;
import com.closiq.payment.domain.Refund;
import com.closiq.payment.service.RefundService;
import com.closiq.seller.service.SellerEarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepositRefundService {

    private final BookingRepository bookingRepository;
    private final RefundService refundService;
    private final BookingTimelineService timelineService;
    private final NotificationDispatchService notificationDispatchService;
    private final ClosiqProperties properties;
    private final SellerEarningService sellerEarningService;

    @Transactional
    public Refund releaseDeposit(
            UUID bookingId,
            UUID initiatedBy,
            long damageDeductionRupees,
            long lateFeeRupees,
            long cleaningFeeRupees,
            String notes,
            String idempotencyKey) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));

        if (!BookingStatus.INSPECTION.equals(booking.getStatus())
                && !BookingStatus.RETURNED.equals(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Deposit refund not allowed in current state");
        }

        long damagePaise = Math.max(0, damageDeductionRupees) * 100;
        long latePaise = Math.max(0, lateFeeRupees) * 100;
        long cleaningPaise = Math.max(0, cleaningFeeRupees) * 100;
        long totalDeductionPaise = damagePaise + latePaise + cleaningPaise;

        booking.setInspectionDamageDeduction(damageDeductionRupees);
        booking.setInspectionLateFee(lateFeeRupees);
        booking.setInspectionCleaningFee(cleaningFeeRupees);
        booking.setInspectionNotes(notes);
        booking.setInspectionCompletedAt(Instant.now());
        if (BookingStatus.RETURNED.equals(booking.getStatus())) {
            booking.setStatus(BookingStatus.INSPECTION);
        }
        bookingRepository.save(booking);

        Payment payment = refundService.resolvePaymentForBooking(booking);

        long depositPaise = booking.getDepositAmount() * 100;
        long refundPaise = depositPaise - totalDeductionPaise;
        if (refundPaise < 0) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Deduction exceeds deposit");
        }

        String reason = buildRefundReason(damageDeductionRupees, lateFeeRupees, cleaningFeeRupees, notes);

        if (refundPaise == 0) {
            booking.setStatus(BookingStatus.DEPOSIT_REFUNDED);
            bookingRepository.save(booking);
            timelineService.append(booking.getId(), initiatedBy, BookingStatus.DEPOSIT_REFUNDED, "Deposit fully withheld after inspection");
            completeBooking(booking, initiatedBy);
            return null;
        }

        Refund refund = refundService.initiateRefund(
                payment.getId(),
                booking.getId(),
                RefundService.TYPE_DEPOSIT,
                refundPaise,
                idempotencyKey != null ? idempotencyKey : "deposit-" + booking.getId(),
                initiatedBy,
                reason,
                properties.getRefund().getDepositExpectedBusinessDays());

        booking.setStatus(BookingStatus.DEPOSIT_REFUNDED);
        bookingRepository.save(booking);
        timelineService.append(
                booking.getId(),
                initiatedBy,
                BookingStatus.DEPOSIT_REFUNDED,
                "Security deposit refund initiated — original payment method");

        notificationDispatchService.depositRefunded(booking, refundPaise);
        completeBooking(booking, initiatedBy);

        return refund;
    }

    @Transactional
    public void beginInspection(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        if (!BookingStatus.RETURNED.equals(booking.getStatus())) {
            return;
        }
        booking.setStatus(BookingStatus.INSPECTION);
        bookingRepository.save(booking);
        timelineService.append(booking.getId(), null, BookingStatus.INSPECTION, "Return received — deposit inspection started");
    }

    private void completeBooking(Booking booking, UUID initiatedBy) {
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());
        bookingRepository.save(booking);
        timelineService.append(booking.getId(), initiatedBy, BookingStatus.COMPLETED, "Rental completed");
        sellerEarningService.creditRentalEarningIfEligible(booking);
    }

    private String buildRefundReason(long damage, long late, long cleaning, String notes) {
        List<String> parts = new ArrayList<>();
        if (damage > 0) {
            parts.add("Damage: ₹" + damage);
        }
        if (late > 0) {
            parts.add("Late return: ₹" + late);
        }
        if (cleaning > 0) {
            parts.add("Cleaning: ₹" + cleaning);
        }
        if (notes != null && !notes.isBlank()) {
            parts.add(notes.trim());
        }
        if (parts.isEmpty()) {
            return "Deposit refund after return inspection";
        }
        return "Deposit refund after inspection — " + String.join("; ", parts);
    }
}
