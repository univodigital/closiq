package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.web.dto.CancelPreviewResponse;
import com.closiq.config.ClosiqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CancellationPolicyServiceTest {

    private CancellationPolicyService service;

    @BeforeEach
    void setUp() {
        ClosiqProperties properties = new ClosiqProperties();
        properties.getBooking().getCancellation().setAfterDispatchRentalRefundPercent(70);
        service = new CancellationPolicyService(properties);
    }

    @Test
    void preDispatch_fullRefund() {
        Booking booking = baseBooking(BookingStatus.CONFIRMED);

        CancelPreviewResponse preview = service.preview(booking);

        assertThat(preview.isEligible()).isTrue();
        assertThat(preview.getPolicyCode()).isEqualTo("PRE_DISPATCH");
        assertThat(preview.getRefundAmount()).isEqualTo(5000);
        assertThat(preview.getNonRefundableAmount()).isZero();
    }

    @Test
    void postDispatch_partialRefund() {
        Booking booking = baseBooking(BookingStatus.OUT_FOR_DELIVERY);
        booking.setRentalAmount(3000);
        booking.setDepositAmount(2000);
        booking.setDeliveryFee(500);
        booking.setTotalAmount(5500);

        CancelPreviewResponse preview = service.preview(booking);

        assertThat(preview.isEligible()).isTrue();
        assertThat(preview.getPolicyCode()).isEqualTo("POST_DISPATCH");
        assertThat(preview.getRentalRefundAmount()).isEqualTo(2100);
        assertThat(preview.getDepositRefundAmount()).isEqualTo(2000);
        assertThat(preview.getNonRefundableAmount()).isEqualTo(1400);
        assertThat(preview.getNonRefundableReason()).contains("dispatch");
    }

    @Test
    void rentalActive_notEligible() {
        Booking booking = baseBooking(BookingStatus.RENTAL_ACTIVE);

        CancelPreviewResponse preview = service.preview(booking);

        assertThat(preview.isEligible()).isFalse();
        assertThat(preview.getRefundAmount()).isZero();
    }

    private Booking baseBooking(String status) {
        return Booking.builder()
                .status(status)
                .rentalAmount(3000)
                .depositAmount(2000)
                .deliveryFee(0)
                .discountAmount(0)
                .totalAmount(5000)
                .build();
    }
}
