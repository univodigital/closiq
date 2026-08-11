package com.closiq.payment.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.domain.CheckoutBatch;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.repository.CheckoutBatchRepository;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String PROVIDER_RAZORPAY = "RAZORPAY";

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final BookingRepository bookingRepository;
    private final CheckoutBatchRepository checkoutBatchRepository;
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
    public CreateRazorpayOrderResponse createBatchRazorpayOrder(
            UUID customerId, String idempotencyKey, UUID checkoutBatchId) {

        holdExpiryService.releaseExpiredHolds();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return toBatchOrderResponse(existing.get());
            }
        }

        CheckoutBatch batch = checkoutBatchRepository.findByIdAndCustomerId(checkoutBatchId, customerId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Checkout batch not found"));

        if (!CheckoutBatch.OPEN.equals(batch.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Checkout is not open for payment");
        }

        if (Instant.now().isAfter(batch.getExpiresAt())) {
            throw new ClosiqException(ErrorCode.BOOKING_CONFLICT, "Checkout hold has expired");
        }

        List<Booking> bookings = bookingRepository.findByCheckoutBatchIdAndCustomerId(checkoutBatchId, customerId);
        if (bookings.isEmpty()) {
            throw new ClosiqException(ErrorCode.NOT_FOUND, "Checkout batch has no bookings");
        }

        for (Booking booking : bookings) {
            if (!BookingStatus.PENDING_PAYMENT.equals(booking.getStatus())) {
                throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Booking is not awaiting payment");
            }
            if (booking.getDeliveryAddressId() == null) {
                throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Delivery address required before payment");
            }
        }

        long amountPaise = batch.getTotalAmount() * 100;
        if (amountPaise < 100) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Minimum payment amount is ₹1");
        }

        Booking primary = bookings.getFirst();
        String receipt = "BATCH-" + batch.getId().toString().substring(0, 8);

        RazorpayOrderResult order;
        try {
            order = paymentGateway.createOrder(amountPaise, batch.getCurrencyCode(), receipt);
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

        long rentalSum = bookings.stream().mapToLong(Booking::getRentalAmount).sum();
        long depositSum = bookings.stream().mapToLong(Booking::getDepositAmount).sum();
        long discountSum = bookings.stream().mapToLong(Booking::getDiscountAmount).sum();

        UUID paymentId = IdGenerator.uuidV7();
        Payment payment = Payment.builder()
                .id(paymentId)
                .bookingId(primary.getId())
                .customerId(customerId)
                .checkoutSessionId(primary.getCheckoutSessionId())
                .checkoutBatchId(checkoutBatchId)
                .providerCode(PROVIDER_RAZORPAY)
                .providerOrderId(order.getProviderOrderId())
                .amount(amountPaise)
                .rentalComponent(rentalSum * 100)
                .depositComponent(depositSum * 100)
                .discountComponent(discountSum * 100)
                .currencyCode(batch.getCurrencyCode())
                .status(PaymentStatus.CREATED)
                .idempotencyKey(idempotencyKey)
                .build();
        paymentRepository.save(payment);

        return CreateRazorpayOrderResponse.builder()
                .paymentId(paymentId.toString())
                .razorpayOrderId(order.getProviderOrderId())
                .amount(order.getAmountPaise())
                .amountInRupees(batch.getTotalAmount())
                .currency(batch.getCurrencyCode())
                .keyId(properties.getRazorpay().getKeyId())
                .bookingId(primary.getId().toString())
                .checkoutBatchId(checkoutBatchId.toString())
                .itemCount(bookings.size())
                .expiresAt(batch.getExpiresAt())
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
        Instant holdExpiry = payment.getCheckoutBatchId() != null
                ? checkoutBatchRepository.findById(payment.getCheckoutBatchId())
                        .map(CheckoutBatch::getExpiresAt)
                        .orElse(booking.getHoldExpiresAt())
                : booking.getHoldExpiresAt();
        if (holdExpiry != null && Instant.now().isAfter(holdExpiry)) {
            throw new ClosiqException(ErrorCode.BOOKING_CONFLICT, "Booking hold expired before payment completed");
        }

        Payment confirmed = payment.getCheckoutBatchId() != null
                ? confirmationService.confirmBatchPayment(payment, request.getRazorpayPaymentId(), "razorpay")
                : confirmationService.confirmPayment(payment, request.getRazorpayPaymentId(), "razorpay");

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

    private CreateRazorpayOrderResponse toBatchOrderResponse(Payment payment) {
        CheckoutBatch batch = checkoutBatchRepository.findById(payment.getCheckoutBatchId()).orElseThrow();
        List<Booking> bookings = bookingRepository.findByCheckoutBatchIdAndCustomerId(
                payment.getCheckoutBatchId(), payment.getCustomerId());
        return CreateRazorpayOrderResponse.builder()
                .paymentId(payment.getId().toString())
                .razorpayOrderId(payment.getProviderOrderId())
                .amount(payment.getAmount())
                .amountInRupees(payment.getAmount() / 100)
                .currency(payment.getCurrencyCode())
                .keyId(properties.getRazorpay().getKeyId())
                .bookingId(payment.getBookingId().toString())
                .checkoutBatchId(payment.getCheckoutBatchId().toString())
                .itemCount(bookings.size())
                .expiresAt(batch.getExpiresAt())
                .build();
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
                .rentalAmount(payment.getRentalComponent() / 100)
                .depositAmount(payment.getDepositComponent() / 100)
                .deliveryFee(booking.getDeliveryFee())
                .discountAmount(payment.getDiscountComponent() / 100)
                .paymentMethod(payment.getPaymentMethod())
                .paidAt(payment.getCapturedAt())
                .checkoutBatchId(payment.getCheckoutBatchId() != null
                        ? payment.getCheckoutBatchId().toString()
                        : null)
                .build();
    }
}
