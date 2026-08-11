package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.web.dto.TrialRejectPreviewResponse;
import com.closiq.config.ClosiqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrialPolicyService {

    private static final String REFUND_METHOD = "ORIGINAL_PAYMENT_METHOD";
    private static final String POLICY_CODE = "TRIAL_REJECT";

    private final ClosiqProperties properties;

    public TrialRejectPreviewResponse previewReject(Booking booking) {
        ClosiqProperties.Cancellation config = properties.getBooking().getCancellation();

        long rentalComponent = Math.max(0, booking.getRentalAmount() - booking.getDiscountAmount());
        long deliveryNonRefundable = booking.getDeliveryFee();

        return TrialRejectPreviewResponse.builder()
                .policyCode(POLICY_CODE)
                .policyLabel("Reject during home trial — no rental charge; deposit refunded after return and inspection")
                .rentalPaid(rentalComponent)
                .rentalRefundAmount(rentalComponent)
                .deliveryFeeNonRefundable(deliveryNonRefundable)
                .depositAmount(booking.getDepositAmount())
                .depositRefundAmount(0)
                .depositRefundTiming(config.getDepositRefundDaysMin() + "–"
                        + config.getDepositRefundDaysMax()
                        + " business days after inspection")
                .refundMethod(REFUND_METHOD)
                .rentalRefundExpectedBusinessDays(config.getRefundBusinessDays())
                .depositRefundExpectedBusinessDaysMin(config.getDepositRefundDaysMin())
                .depositRefundExpectedBusinessDaysMax(config.getDepositRefundDaysMax())
                .build();
    }

    public boolean isTrialDecisionAllowed(Booking booking) {
        return BookingStatus.TRIAL_READY.equals(booking.getStatus());
    }
}
