package com.closiq.payment.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.util.IdGenerator;
import com.closiq.config.ClosiqProperties;
import com.closiq.payment.domain.Payment;
import com.closiq.payment.domain.PaymentStatus;
import com.closiq.payment.domain.Refund;
import com.closiq.payment.gateway.PaymentGateway;
import com.closiq.payment.gateway.RazorpayApiException;
import com.closiq.payment.gateway.RazorpayRefundResult;
import com.closiq.payment.repository.PaymentRepository;
import com.closiq.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    public static final String TYPE_RENTAL = "RENTAL";
    public static final String TYPE_DEPOSIT = "DEPOSIT";
    public static final String TYPE_FULL = "FULL";
    public static final String TYPE_PARTIAL = "PARTIAL";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_FAILED = "FAILED";

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentGateway paymentGateway;
    private final ClosiqProperties properties;

    @Transactional
    public Refund initiateRefund(
            UUID paymentId,
            UUID bookingId,
            String refundType,
            long amountPaise,
            String idempotencyKey,
            UUID initiatedBy,
            String reason,
            int expectedBusinessDays) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = refundRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Payment not found"));

        if (!PaymentStatus.CAPTURED.equals(payment.getStatus())
                && !PaymentStatus.PARTIALLY_REFUNDED.equals(payment.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Payment is not refundable");
        }

        if (payment.getProviderPaymentId() == null || payment.getProviderPaymentId().isBlank()) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Payment has no provider reference for refund");
        }

        long alreadyRefunded = refundedAmountPaise(paymentId);
        if (alreadyRefunded + amountPaise > payment.getAmount()) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Refund exceeds refundable amount");
        }

        UUID refundId = IdGenerator.uuidV7();
        Instant now = Instant.now();
        Refund refund = Refund.builder()
                .id(refundId)
                .paymentId(paymentId)
                .bookingId(bookingId)
                .initiatedBy(initiatedBy)
                .refundType(refundType)
                .amount(amountPaise)
                .status(STATUS_PROCESSING)
                .reason(reason)
                .initiatedAt(now)
                .expectedBy(now.plus(expectedBusinessDays, ChronoUnit.DAYS))
                .idempotencyKey(idempotencyKey)
                .build();
        refundRepository.save(refund);

        try {
            RazorpayRefundResult providerResult = paymentGateway.createRefund(
                    payment.getProviderPaymentId(), amountPaise, idempotencyKey);

            refund.setProviderRefundId(providerResult.getProviderRefundId());
            refund.setStatus(mapProviderRefundStatus(providerResult.getStatus()));
            if (STATUS_PROCESSED.equals(refund.getStatus())) {
                refund.setProcessedAt(Instant.now());
            }
            refundRepository.save(refund);

            updatePaymentRefundStatus(payment, amountPaise);
            return refund;
        } catch (RazorpayApiException ex) {
            refund.setStatus(STATUS_FAILED);
            refundRepository.save(refund);
            log.warn("Refund provider error for payment {}: {}", paymentId, ex.getMessage());
            throw new ClosiqException(ErrorCode.INTERNAL_ERROR, "Refund could not be processed");
        }
    }

    @Transactional(readOnly = true)
    public long refundedAmountPaise(UUID paymentId) {
        return refundRepository.findByPaymentIdOrderByInitiatedAtAsc(paymentId).stream()
                .filter(r -> !STATUS_FAILED.equals(r.getStatus()))
                .mapToLong(Refund::getAmount)
                .sum();
    }

    @Transactional(readOnly = true)
    public long remainingRefundablePaise(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        return payment.getAmount() - refundedAmountPaise(paymentId);
    }

    @Transactional
    public Refund initiateBookingRefund(
            Booking booking,
            String refundType,
            UUID initiatedBy,
            String reason,
            String idempotencyKey) {

        Payment payment = resolvePaymentForBooking(booking);
        long amountPaise = booking.getTotalAmount() * 100;
        if (TYPE_FULL.equals(refundType)) {
            amountPaise = Math.min(amountPaise, remainingRefundablePaise(payment.getId()));
        }

        return initiateRefund(
                payment.getId(),
                booking.getId(),
                refundType,
                amountPaise,
                idempotencyKey != null ? idempotencyKey : refundType.toLowerCase() + "-" + booking.getId(),
                initiatedBy,
                reason,
                properties.getBooking().getCancellation().getRefundBusinessDays());
    }

    public Payment resolvePaymentForBooking(Booking booking) {
        if (booking.getCheckoutBatchId() != null) {
            return paymentRepository.findByCheckoutBatchId(booking.getCheckoutBatchId())
                    .filter(p -> PaymentStatus.CAPTURED.equals(p.getStatus())
                            || PaymentStatus.PARTIALLY_REFUNDED.equals(p.getStatus()))
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "No captured payment for booking"));
        }
        return paymentRepository.findByBookingIdAndStatus(booking.getId(), PaymentStatus.CAPTURED)
                .or(() -> paymentRepository.findByBookingIdAndStatus(booking.getId(), PaymentStatus.PARTIALLY_REFUNDED))
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "No captured payment for booking"));
    }

    @Transactional
    public Refund initiateFullBookingRefund(Booking booking, UUID initiatedBy, String reason, String idempotencyKey) {
        Payment payment = resolvePaymentForBooking(booking);

        long amount = remainingRefundablePaise(payment.getId());
        if (amount <= 0) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Nothing left to refund");
        }

        Refund refund = initiateRefund(
                payment.getId(),
                booking.getId(),
                TYPE_FULL,
                amount,
                idempotencyKey != null ? idempotencyKey : "full-" + booking.getId(),
                initiatedBy,
                reason,
                properties.getBooking().getCancellation().getRefundBusinessDays());

        booking.setStatus(BookingStatus.REFUND_PENDING);
        bookingRepository.save(booking);
        return refund;
    }

    private void updatePaymentRefundStatus(Payment payment, long refundAmountPaise) {
        long totalRefunded = refundedAmountPaise(payment.getId());
        if (totalRefunded >= payment.getAmount()) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else if (totalRefunded > 0) {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }
        paymentRepository.save(payment);
    }

    private static String mapProviderRefundStatus(String providerStatus) {
        if (providerStatus == null) {
            return STATUS_PROCESSING;
        }
        return switch (providerStatus.toLowerCase()) {
            case "processed" -> STATUS_PROCESSED;
            case "failed" -> STATUS_FAILED;
            default -> STATUS_PROCESSING;
        };
    }
}
