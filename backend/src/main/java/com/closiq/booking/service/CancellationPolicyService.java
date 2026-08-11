package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.web.dto.CancelPreviewResponse;
import com.closiq.config.ClosiqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CancellationPolicyService {

    private static final String REFUND_METHOD = "ORIGINAL_PAYMENT_METHOD";

    private static final Set<String> PRE_DISPATCH = Set.of(
            BookingStatus.CONFIRMED,
            BookingStatus.SELLER_ACCEPTED,
            BookingStatus.PREPARING);

    private static final Set<String> POST_DISPATCH = Set.of(BookingStatus.OUT_FOR_DELIVERY);

    private static final Set<String> NO_CANCEL = Set.of(
            BookingStatus.PENDING_PAYMENT,
            BookingStatus.TRIAL_READY,
            BookingStatus.TRIAL_REJECTED,
            BookingStatus.RENTAL_ACTIVE,
            BookingStatus.RETURN_SCHEDULED,
            BookingStatus.RETURN_IN_TRANSIT,
            BookingStatus.RETURNED,
            BookingStatus.INSPECTION,
            BookingStatus.DEPOSIT_REFUNDED,
            BookingStatus.COMPLETED,
            BookingStatus.CANCELLED,
            BookingStatus.REFUND_PENDING);

    private final ClosiqProperties properties;

    public boolean isCancellationEligible(Booking booking) {
        return PRE_DISPATCH.contains(booking.getStatus()) || POST_DISPATCH.contains(booking.getStatus());
    }

    public CancelPreviewResponse preview(Booking booking) {
        ClosiqProperties.Cancellation config = properties.getBooking().getCancellation();
        String status = booking.getStatus();

        if (BookingStatus.PENDING_PAYMENT.equals(status)) {
            return CancelPreviewResponse.builder()
                    .eligible(true)
                    .policyCode("PRE_PAYMENT")
                    .policyLabel("Cancel before payment — no charge")
                    .originalAmount(0)
                    .refundAmount(0)
                    .nonRefundableAmount(0)
                    .rentalRefundAmount(0)
                    .depositRefundAmount(0)
                    .deliveryFeeNonRefundable(0)
                    .refundMethod(REFUND_METHOD)
                    .expectedRefundBusinessDays(0)
                    .build();
        }

        if (!isCancellationEligible(booking)) {
            return CancelPreviewResponse.builder()
                    .eligible(false)
                    .policyCode("NOT_ALLOWED")
                    .policyLabel("Cancellation is not available at this stage")
                    .originalAmount(booking.getTotalAmount())
                    .refundAmount(0)
                    .nonRefundableAmount(booking.getTotalAmount())
                    .rentalRefundAmount(0)
                    .depositRefundAmount(0)
                    .deliveryFeeNonRefundable(0)
                    .refundMethod(REFUND_METHOD)
                    .expectedRefundBusinessDays(0)
                    .build();
        }

        if (PRE_DISPATCH.contains(status)) {
            return CancelPreviewResponse.builder()
                    .eligible(true)
                    .policyCode("PRE_DISPATCH")
                    .policyLabel("Cancel before dispatch: Full refund")
                    .originalAmount(booking.getTotalAmount())
                    .refundAmount(booking.getTotalAmount())
                    .nonRefundableAmount(0)
                    .rentalRefundAmount(booking.getRentalAmount() - booking.getDiscountAmount())
                    .depositRefundAmount(booking.getDepositAmount())
                    .deliveryFeeNonRefundable(0)
                    .refundMethod(REFUND_METHOD)
                    .expectedRefundBusinessDays(config.getRefundBusinessDays())
                    .build();
        }

        // POST_DISPATCH — partial refund: delivery fee non-refundable, partial rental
        long rentalComponent = Math.max(0, booking.getRentalAmount() - booking.getDiscountAmount());
        long rentalRefund = (rentalComponent * config.getAfterDispatchRentalRefundPercent()) / 100;
        long deliveryNonRefundable = booking.getDeliveryFee();
        long depositRefund = config.isDepositFullyRefundableOnCancel() ? booking.getDepositAmount() : 0;
        long refundAmount = rentalRefund + depositRefund;
        long nonRefundable = booking.getTotalAmount() - refundAmount;

        return CancelPreviewResponse.builder()
                .eligible(true)
                .policyCode("POST_DISPATCH")
                .policyLabel("Cancellation after dispatch: Partial refund")
                .originalAmount(booking.getTotalAmount())
                .refundAmount(refundAmount)
                .nonRefundableAmount(nonRefundable)
                .rentalRefundAmount(rentalRefund)
                .depositRefundAmount(depositRefund)
                .deliveryFeeNonRefundable(deliveryNonRefundable)
                .nonRefundableReason("Delivery/dispatch costs already incurred")
                .refundMethod(REFUND_METHOD)
                .expectedRefundBusinessDays(config.getRefundBusinessDays())
                .build();
    }
}
