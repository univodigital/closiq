package com.closiq.notification.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTypeTest {

    @Test
    void constants_matchPhase3Contract() {
        assertThat(com.closiq.notification.domain.NotificationType.BOOKING_CONFIRMED)
                .isEqualTo("BOOKING_CONFIRMED");
        assertThat(com.closiq.notification.domain.NotificationType.TRIAL_READY)
                .isEqualTo("TRIAL_READY");
        assertThat(com.closiq.notification.domain.NotificationType.SELLER_PAYOUT)
                .isEqualTo("SELLER_PAYOUT");
    }
}
