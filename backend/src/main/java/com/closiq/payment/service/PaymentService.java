package com.closiq.payment.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.service.BookingHoldExpiryService;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.IdGenerator;
import com.closiq.config.ClosiqProperties;
import com.closiq.payment.domain.Payment;
import com.closiq.payment.domain.PaymentAttempt;
import com.closiq.payment.domain.PaymentStatus;
import com.closiq.payment.gateway.PaymentGateway;
import com.closiq.payment.gateway.RazorpayApiException;
import com.closiq.payment.gateway.RazorpayOrderResult;
import com.closiq.payment.repository.PaymentAttemptRepository;
import com.closiq.payment.repository.PaymentRepository;
import com.closiq.payment.web.dto.CreateRazorpayOrderResponse;
import com.closiq.payment.web.dto.VerifyPaymentRequest;
import com.closiq.payment.web.dto.VerifyPaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String PROVIDER_RAZORPAY = "RAZORPAY";

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final BookingRepository bookingRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentConfirmationService confirmationService;
    private final BookingHoldExpiryService holdExpiryService;
    private final ClosiqProperties properties;

    @Transactional
    public CreateRazorpayOrderResponse createRazorpayOrder(
            UUID customerId, String idempotencyKey, UUID bookingId, UUID checkoutSessionId) {

        holdExpiryService.releaseExpiredHolds();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return toOrderResponse(existing.get());
            }
        }

        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customerId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));

        if (!BookingStatus.PENDING_PAYMENT.equals(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Booking is not awaiting payment");
        }

        if (booking.getHoldExpiresAt() != null && Instant.now().isAfter(booking.getHoldExpiresAt())) {
            throw new ClosiqException(ErrorCode.BOOKING_CONFLICT, "Booking hold has expired");
        }

        if (checkoutSessionId != null && !checkoutSessionId.equals(booking.getCheckoutSessionId())) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Checkout session mismatch");
        }

        if (booking.getDeliveryAddressId() == null) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Delivery address required before payment");
        }

        long amountPaise = booking.getTotalAmount() * 100;
        if (amountPaise < 100) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Minimum payment amount is ₹1");
        }

        RazorpayOrderResult order;
        try {
            order = paymentGateway.createOrder(amountPaise, booking.getCurrencyCode(), booking.getRentalNumber());
        } catch (IllegalArgumentException ex) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        } catch (RazorpayApiException ex) {
            if (ex.getStatusCode() == 401) {
                throw new ClosiqException(ErrorCode.UNAUTHORIZED, "Razorpay authentication failed");
            }
            throw new ClosiqException(ErrorCode.INTERNAL_ERROR, "Razorpay order creation failed");
        } catch (Exception ex) {
            throw new ClosiqException(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE);
        }

        UUID paymentId = IdGenerator.uuidV7();
        Payment payment = Payment.builder()
                .id(paymentId)
                .bookingId(booking.getId())
                .customerId(customerId)
                .checkoutSessionId(booking.getCheckoutSessionId())
                .providerCode(PROVIDER_RAZORPAY)
                .providerOrderId(order.getProviderOrderId())
                .amount(amountPaise)
                .rentalComponent(booking.getRentalAmount() * 100)
                .depositComponent(booking.getDepositAmount() * 100)
                .discountComponent(booking.getDiscountAmount() * 100)
                .currencyCode(booking.getCurrencyCode())
                .status(PaymentStatus.CREATED)
                .idempotencyKey(idempotencyKey)
                .build();
        paymentRepository.save(payment);

        return CreateRazorpayOrderResponse.builder()
                .paymentId(paymentId.toString())
                .razorpayOrderId(order.getProviderOrderId())
                .amount(order.getAmountPaise())
                .amountInRupees(booking.getTotalAmount())
                .currency(booking.getCurrencyCode())
                .keyId(properties.getRazorpay().getKeyId())
                .bookingId(booking.getId().toString())
                .expiresAt(booking.getHoldExpiresAt())
                .build();
    }

    @Transactional
    public VerifyPaymentResponse verifyPayment(UUID customerId, VerifyPaymentRequest request) {
        Payment payment = paymentRepository.findByIdAndCustomerId(UUID.fromString(request.getPaymentId()), customerId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Payment not found"));

        if (PaymentStatus.CAPTURED.equals(payment.getStatus())) {
            return toVerifyResponse(payment);
        }

        if (!payment.getProviderOrderId().equals(request.getRazorpayOrderId())) {
            recordFailedAttempt(payment, "ORDER_MISMATCH", "Razorpay order ID mismatch");
            throw new ClosiqException(ErrorCode.PAYMENT_FAILED, "Order ID mismatch");
        }

        boolean valid = paymentGateway.verifyPaymentSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailedAt(Instant.now());
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);
            recordFailedAttempt(payment, "SIGNATURE_INVALID", "Invalid Razorpay signature");
            throw new ClosiqException(ErrorCode.PAYMENT_FAILED);
        }

        Booking booking = bookingRepository.findById(payment.getBookingId()).orElseThrow();
        if (booking.getHoldExpiresAt() != null && Instant.now().isAfter(booking.getHoldExpiresAt())) {
            throw new ClosiqException(ErrorCode.BOOKING_CONFLICT, "Booking hold expired before payment completed");
        }

        Payment confirmed = confirmationService.confirmPayment(
                payment, request.getRazorpayPaymentId(), "razorpay");

        return toVerifyResponse(confirmed);
    }

    private void recordFailedAttempt(Payment payment, String code, String message) {
        paymentAttemptRepository.save(PaymentAttempt.builder()
                .paymentId(payment.getId())
                .providerOrderId(payment.getProviderOrderId())
                .errorCode(code)
                .errorMessage(message)
                .attemptedAt(Instant.now())
                .build());
    }

    private CreateRazorpayOrderResponse toOrderResponse(Payment payment) {
        Booking booking = bookingRepository.findById(payment.getBookingId()).orElseThrow();
        return CreateRazorpayOrderResponse.builder()
                .paymentId(payment.getId().toString())
                .razorpayOrderId(payment.getProviderOrderId())
                .amount(payment.getAmount())
                .amountInRupees(payment.getAmount() / 100)
                .currency(payment.getCurrencyCode())
                .keyId(properties.getRazorpay().getKeyId())
                .bookingId(payment.getBookingId().toString())
                .expiresAt(booking.getHoldExpiresAt())
                .build();
    }

    private VerifyPaymentResponse toVerifyResponse(Payment payment) {
        Booking booking = bookingRepository.findById(payment.getBookingId()).orElseThrow();
        String rentalNumber = booking.getRentalNumber();
        return VerifyPaymentResponse.builder()
                .paymentId(payment.getId().toString())
                .status(payment.getStatus())
                .bookingId(payment.getBookingId().toString())
                .rentalNumber(rentalNumber)
                .bookingNumber(rentalNumber)
                .orderNumber(booking.getOrderNumber())
                .bookingStatus(booking.getStatus())
                .paidAmount(payment.getAmount() / 100)
                .currency(payment.getCurrencyCode())
                .build();
    }
}
