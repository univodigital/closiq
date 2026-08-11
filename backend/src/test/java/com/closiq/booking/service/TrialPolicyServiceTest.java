package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.config.ClosiqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrialPolicyServiceTest {

    private TrialPolicyService trialPolicyService;

    @BeforeEach
    void setUp() {
        ClosiqProperties properties = new ClosiqProperties();
        properties.getBooking().getCancellation().setRefundBusinessDays(5);
        properties.getBooking().getCancellation().setDepositRefundDaysMin(5);
        properties.getBooking().getCancellation().setDepositRefundDaysMax(7);
        trialPolicyService = new TrialPolicyService(properties);
    }

    @Test
    void previewReject_refundsFullRentalNotDeposit() {
        Booking booking = Booking.builder()
                .status(BookingStatus.TRIAL_READY)
                .rentalAmount(3000)
                .discountAmount(500)
                .depositAmount(2000)
                .deliveryFee(0)
                .build();

        var preview = trialPolicyService.previewReject(booking);

        assertThat(preview.getRentalRefundAmount()).isEqualTo(2500);
        assertThat(preview.getDepositRefundAmount()).isZero();
        assertThat(preview.getDepositAmount()).isEqualTo(2000);
        assertThat(preview.getRentalRefundExpectedBusinessDays()).isEqualTo(5);
    }
}
