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
import com.closiq.payment.web.dto.CheckoutCalculateBatchRequest;
import com.closiq.payment.web.dto.CheckoutCalculateRequest;
import com.closiq.payment.web.dto.CheckoutCalculateResponse;
import com.closiq.payment.web.dto.CheckoutSessionResponse;
import com.closiq.payment.web.dto.InitiateCheckoutSessionRequest;
import com.closiq.payment.web.dto.ValidateCouponResponse;
import com.closiq.inventory.service.AvailabilityService;
import com.closiq.user.domain.Address;
import com.closiq.user.repository.AddressRepository;
import com.closiq.user.repository.ServiceablePincodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final AvailabilityService availabilityService;

    @Transactional(readOnly = true)
    public CheckoutCalculateResponse calculate(CheckoutCalculateRequest request) {
        Product product = productRepository.findByIdAndDeletedAtIsNullAndStatus(request.getProductId(), ACTIVE_PRODUCT)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Product not found"));

        productVariantRepository.findByIdAndProductId(request.getVariantId(), product.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Variant not found"));

        LocalDate effectiveEnd = request.getRentalEndDate().plusDays(product.getCleaningBufferDays());
        if (!availabilityService.isRangeAvailable(
                request.getVariantId(), request.getRentalStartDate(), effectiveEnd)) {
            throw new ClosiqException(
                    ErrorCode.BOOKING_CONFLICT,
                    "Selected dates are no longer available for this item");
        }

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
    public CheckoutCalculateResponse calculateBatch(CheckoutCalculateBatchRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "At least one item is required");
        }

        List<CheckoutCalculateResponse> itemResponses = new ArrayList<>();
        long combinedSubtotal = 0;

        for (CheckoutCalculateBatchRequest.LineItem line : request.getItems()) {
            CheckoutCalculateResponse item = calculate(new CheckoutCalculateRequest(
                    line.getProductId(),
                    line.getVariantId(),
                    line.getRentalStartDate(),
                    line.getRentalEndDate(),
                    request.getPincode(),
                    null));
            itemResponses.add(item);
            combinedSubtotal += item.getSubtotal();
        }

        long discount = 0;
        String couponCode = request.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            discount = couponService.validate(couponCode, combinedSubtotal).discountAmount();
        }

        long total = combinedSubtotal - discount;
        boolean serviceable = itemResponses.stream().allMatch(r -> r.isServiceable());

        List<CheckoutCalculateResponse.LineItem> lineItems = new ArrayList<>();
        Map<String, CheckoutCalculateResponse.LineItem> aggregated = new LinkedHashMap<>();

        for (int i = 0; i < itemResponses.size(); i++) {
            CheckoutCalculateResponse item = itemResponses.get(i);
            for (CheckoutCalculateResponse.LineItem li : item.getLineItems()) {
                if ("RENTAL".equals(li.getType())) {
                    lineItems.add(CheckoutCalculateResponse.LineItem.builder()
                            .type(li.getType())
                            .label(itemResponses.size() > 1
                                    ? "Item " + (i + 1) + ": " + li.getLabel()
                                    : li.getLabel())
                            .amount(li.getAmount())
                            .build());
                } else if (!"DISCOUNT".equals(li.getType())) {
                    CheckoutCalculateResponse.LineItem existing = aggregated.get(li.getType());
                    if (existing == null) {
                        aggregated.put(li.getType(), li);
                    } else {
                        aggregated.put(li.getType(), CheckoutCalculateResponse.LineItem.builder()
                                .type(li.getType())
                                .label(existing.getLabel())
                                .amount(existing.getAmount() + li.getAmount())
                                .build());
                    }
                }
            }
        }
        lineItems.addAll(aggregated.values());

        if (discount > 0) {
            lineItems.add(CheckoutCalculateResponse.LineItem.builder()
                    .type("DISCOUNT")
                    .label("Coupon " + couponCode)
                    .amount(-discount)
                    .build());
        }

        CheckoutCalculateResponse first = itemResponses.getFirst();
        return CheckoutCalculateResponse.builder()
                .rentalDays((short) itemResponses.stream().mapToInt(CheckoutCalculateResponse::getRentalDays).max().orElse(0))
                .lineItems(lineItems)
                .subtotal(combinedSubtotal)
                .discountAmount(discount)
                .totalAmount(total)
                .depositAmount(itemResponses.stream().mapToLong(CheckoutCalculateResponse::getDepositAmount).sum())
                .payNowAmount(total)
                .currency(first.getCurrency())
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
