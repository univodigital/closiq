package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingStatusTransitionsTest {

    @Test
    void allowsConfirmedToSellerAccepted() {
        BookingStatusTransitions.assertTransition(BookingStatus.CONFIRMED, BookingStatus.SELLER_ACCEPTED);
    }

    @Test
    void rejectsCompletedToConfirmed() {
        assertThatThrownBy(() ->
                BookingStatusTransitions.assertTransition(BookingStatus.COMPLETED, BookingStatus.CONFIRMED))
                .isInstanceOf(ClosiqException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATE_TRANSITION);
    }

    @Test
    void rejectsDeliveredToSellerPreparing() {
        assertThatThrownBy(() ->
                BookingStatusTransitions.assertTransition(
                        BookingStatus.TRIAL_READY, BookingStatus.PREPARING))
                .isInstanceOf(ClosiqException.class);
    }

    @Test
    void pipelineRankOrdersLifecycle() {
        assertThat(BookingStatusTransitions.pipelineRank(BookingStatus.CONFIRMED))
                .isLessThan(BookingStatusTransitions.pipelineRank(BookingStatus.OUT_FOR_DELIVERY));
        assertThat(BookingStatusTransitions.pipelineRank(BookingStatus.RENTAL_ACTIVE))
                .isLessThan(BookingStatusTransitions.pipelineRank(BookingStatus.COMPLETED));
    }
}
