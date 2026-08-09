package com.closiq.payment.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.domain.CheckoutSession;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.repository.CheckoutSessionRepository;
import com.closiq.booking.service.BookingHoldExpiryService;
import com.closiq.booking.service.BookingPricingService;
import com.closiq.booking.service.BookingService;
import com.closiq.catalog.domain.Product;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.catalog.repository.ProductVariantRepository;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.payment.web.dto.CheckoutCalculateRequest;
import com.closiq.payment.web.dto.CheckoutCalculateResponse;
import com.closiq.payment.web.dto.CheckoutSessionResponse;
import com.closiq.payment.web.dto.InitiateCheckoutSessionRequest;
import com.closiq.payment.web.dto.ValidateCouponResponse;
import com.closiq.user.domain.Address;
import com.closiq.user.repository.AddressRepository;
import com.closiq.user.repository.ServiceablePincodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final String ACTIVE_PRODUCT = "ACTIVE";
    private static final String ACTIVE_PINCODE = "ACTIVE";
    private static final String OPEN = "OPEN";

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final BookingPricingService pricingService;
    private final CouponService couponService;
    private final ServiceablePincodeRepository serviceablePincodeRepository;
    private final BookingRepository bookingRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final AddressRepository addressRepository;
    private final BookingService bookingService;
    private final BookingHoldExpiryService holdExpiryService;

    @Transactional(readOnly = true)
    public CheckoutCalculateResponse calculate(CheckoutCalculateRequest request) {
        Product product = productRepository.findByIdAndDeletedAtIsNullAndStatus(request.getProductId(), ACTIVE_PRODUCT)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Product not found"));

        productVariantRepository.findByIdAndProductId(request.getVariantId(), product.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Variant not found"));

        BookingPricingService.PricingBreakdown pricing = pricingService.calculate(
                product, request.getRentalStartDate(), request.getRentalEndDate());

        long subtotal = pricing.getRentalAmount() + pricing.getDepositAmount() + pricing.getDeliveryFee();
        long discount = 0;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            discount = couponService.validate(request.getCouponCode(), subtotal).discountAmount();
        }

        long total = subtotal - discount;
        boolean serviceable = request.getPincode() == null
                || serviceablePincodeRepository.findByPincodeAndStatus(request.getPincode(), ACTIVE_PINCODE).isPresent();

        List<CheckoutCalculateResponse.LineItem> lineItems = new ArrayList<>();
        lineItems.add(CheckoutCalculateResponse.LineItem.builder()
                .type("RENTAL")
                .label("Rental (" + pricing.getRentalDays() + " days × ₹" + product.getPricePerDay() + ")")
                .amount(pricing.getRentalAmount())
                .build());
        lineItems.add(CheckoutCalculateResponse.LineItem.builder()
                .type("DEPOSIT")
                .label("Refundable deposit")
                .amount(pricing.getDepositAmount())
                .build());
        lineItems.add(CheckoutCalculateResponse.LineItem.builder()
                .type("DELIVERY")
                .label("Delivery")
                .amount(pricing.getDeliveryFee())
                .build());
        if (discount > 0) {
            lineItems.add(CheckoutCalculateResponse.LineItem.builder()
                    .type("DISCOUNT")
                    .label("Coupon " + request.getCouponCode())
                    .amount(-discount)
                    .build());
        }

        return CheckoutCalculateResponse.builder()
                .rentalDays(pricing.getRentalDays())
                .lineItems(lineItems)
                .subtotal(subtotal)
                .discountAmount(discount)
                .totalAmount(total)
                .depositAmount(pricing.getDepositAmount())
                .payNowAmount(total)
                .currency(pricing.getCurrency())
                .serviceable(serviceable)
                .build();
    }

    @Transactional(readOnly = true)
    public ValidateCouponResponse validateCoupon(UUID customerId, String couponCode, UUID bookingId) {
        long subtotal;
        if (bookingId != null) {
            Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customerId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
            subtotal = booking.getRentalAmount() + booking.getDepositAmount() + booking.getDeliveryFee();
        } else {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "bookingId is required");
        }
        var validation = couponService.validate(couponCode, subtotal);
        return ValidateCouponResponse.builder()
                .valid(true)
                .discountAmount(validation.discountAmount())
                .message(validation.message())
                .build();
    }

    @Transactional
    public CheckoutSessionResponse initiateSession(UUID customerId, InitiateCheckoutSessionRequest request) {
        holdExpiryService.releaseExpiredHolds();

        Booking booking = bookingRepository.findByIdAndCustomerId(request.getBookingId(), customerId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));

        if (!BookingStatus.PENDING_PAYMENT.equals(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        if (booking.getHoldExpiresAt() != null && Instant.now().isAfter(booking.getHoldExpiresAt())) {
            throw new ClosiqException(ErrorCode.BOOKING_CONFLICT, "Booking hold has expired");
        }

        Address address = addressRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getDeliveryAddressId(), customerId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Address not found"));

        if (!serviceablePincodeRepository.findByPincodeAndStatus(address.getPincode(), ACTIVE_PINCODE).isPresent()) {
            throw new ClosiqException(ErrorCode.PINCODE_NOT_SERVICEABLE);
        }

        long subtotal = booking.getRentalAmount() + booking.getDepositAmount() + booking.getDeliveryFee();
        long discount = 0;
        String couponCode = request.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            discount = couponService.validate(couponCode, subtotal).discountAmount();
        }

        booking.setDeliveryAddressId(address.getId());
        booking.setDiscountAmount(discount);
        booking.setTotalAmount(subtotal - discount);
        bookingRepository.save(booking);

        CheckoutSession session = checkoutSessionRepository.findById(booking.getCheckoutSessionId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Checkout session not found"));

        session.setDeliveryAddressId(address.getId());
        session.setCouponCode(couponCode);
        session.setDiscountAmount(discount);
        session.setReadyForPayment(true);
        session.setStatus(OPEN);
        checkoutSessionRepository.save(session);

        return CheckoutSessionResponse.builder()
                .sessionId(session.getId().toString())
                .bookingId(booking.getId().toString())
                .readyForPayment(true)
                .totalAmount(booking.getTotalAmount())
                .discountAmount(discount)
                .currency(booking.getCurrencyCode())
                .booking(bookingService.getBooking(customerId, booking.getId().toString()))
                .build();
    }

    @Transactional(readOnly = true)
    public CheckoutSessionResponse getSummary(UUID customerId, UUID sessionId) {
        CheckoutSession session = checkoutSessionRepository.findByIdAndCustomerId(sessionId, customerId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Checkout session not found"));

        if (session.getBookingId() == null) {
            throw new ClosiqException(ErrorCode.NOT_FOUND, "Checkout session has no booking");
        }

        Booking booking = bookingRepository.findByIdAndCustomerId(session.getBookingId(), customerId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));

        return CheckoutSessionResponse.builder()
                .sessionId(session.getId().toString())
                .bookingId(booking.getId().toString())
                .readyForPayment(session.isReadyForPayment())
                .totalAmount(booking.getTotalAmount())
                .discountAmount(session.getDiscountAmount())
                .currency(booking.getCurrencyCode())
                .booking(bookingService.getBooking(customerId, booking.getId().toString()))
                .build();
    }
}
