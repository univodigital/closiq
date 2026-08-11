package com.closiq.payment.service;

import com.closiq.booking.repository.BookingRepository;
import com.closiq.config.ClosiqProperties;
import com.closiq.payment.domain.Payment;
import com.closiq.payment.domain.PaymentStatus;
import com.closiq.payment.gateway.PaymentGateway;
import com.closiq.payment.repository.PaymentRepository;
import com.closiq.payment.repository.RefundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();

    @Mock private RefundRepository refundRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentGateway paymentGateway;
    @Mock private ClosiqProperties properties;

    @InjectMocks
    private RefundService refundService;

    @Test
    void refundedAmountPaise_excludesFailedRefunds() {
        when(refundRepository.findByPaymentIdOrderByInitiatedAtAsc(PAYMENT_ID)).thenReturn(List.of(
                com.closiq.payment.domain.Refund.builder()
                        .amount(50_000L)
                        .status(RefundService.STATUS_PROCESSED)
                        .build(),
                com.closiq.payment.domain.Refund.builder()
                        .amount(10_000L)
                        .status(RefundService.STATUS_FAILED)
                        .build()));

        assertThat(refundService.refundedAmountPaise(PAYMENT_ID)).isEqualTo(50_000L);
    }

    @Test
    void remainingRefundablePaise_accountsForProcessedRefunds() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(
                Payment.builder().id(PAYMENT_ID).amount(100_000L).status(PaymentStatus.CAPTURED).build()));
        when(refundRepository.findByPaymentIdOrderByInitiatedAtAsc(PAYMENT_ID)).thenReturn(List.of(
                com.closiq.payment.domain.Refund.builder()
                        .amount(30_000L)
                        .status(RefundService.STATUS_PROCESSED)
                        .build()));

        assertThat(refundService.remainingRefundablePaise(PAYMENT_ID)).isEqualTo(70_000L);
    }
}
