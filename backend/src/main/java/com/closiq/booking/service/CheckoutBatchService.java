package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingItem;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.domain.CheckoutBatch;
import com.closiq.booking.domain.CheckoutSession;
import com.closiq.booking.repository.BookingItemRepository;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.repository.CheckoutBatchRepository;
import com.closiq.booking.repository.CheckoutSessionRepository;
import com.closiq.booking.web.dto.CreateBookingRequest;
import com.closiq.booking.web.dto.CreateBookingResponse;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.util.IdGenerator;
import com.closiq.config.ClosiqProperties;
import com.closiq.inventory.service.InventoryHoldService;
import com.closiq.payment.service.CouponService;
import com.closiq.payment.web.dto.PrepareCheckoutBatchRequest;
import com.closiq.payment.web.dto.PrepareCheckoutBatchResponse;
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
public class CheckoutBatchService {

    private static final String ACTIVE_PINCODE = "ACTIVE";
    private static final String OPEN_CHECKOUT = "OPEN";

    private final CheckoutBatchRepository checkoutBatchRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final BookingService bookingService;
    private final BookingHoldExpiryService holdExpiryService;
    private final AddressRepository addressRepository;
    private final ServiceablePincodeRepository serviceablePincodeRepository;
    private final CouponService couponService;
    private final ClosiqProperties properties;
    private final InventoryHoldService inventoryHoldService;
    private final BookingTimelineService timelineService;

    @Transactional
    public PrepareCheckoutBatchResponse prepare(
            UUID customerId, String idempotencyKey, PrepareCheckoutBatchRequest request) {

        holdExpiryService.releaseExpiredHolds();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = checkoutBatchRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                CheckoutBatch batch = existing.get();
                if (canReuseBatch(batch, customerId, request)) {
                    return rebuildResponse(batch, customerId);
                }
                abandonBatch(batch);
            }
        }

        releaseOtherOpenCheckouts(customerId);

        Address address = addressRepository.findByIdAndUserIdAndDeletedAtIsNull(
                        request.getDeliveryAddressId(), customerId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Address not found"));

        if (!serviceablePincodeRepository.findByPincodeAndStatus(address.getPincode(), ACTIVE_PINCODE).isPresent()) {
            throw new ClosiqException(ErrorCode.PINCODE_NOT_SERVICEABLE);
        }

        List<CreateBookingResponse> holds = new ArrayList<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            PrepareCheckoutBatchRequest.LineItem line = request.getItems().get(i);
            String itemKey = idempotencyKey != null ? idempotencyKey + "-item-" + i : null;
            CreateBookingRequest bookingRequest = new CreateBookingRequest(
                    line.getProductId(),
                    line.getVariantId(),
                    line.getRentalStartDate(),
                    line.getRentalEndDate(),
                    request.getDeliveryAddressId(),
                    null);
            holds.add(bookingService.createHold(customerId, itemKey, bookingRequest));
        }

        long combinedSubtotal = 0;
        List<Booking> bookings = new ArrayList<>();
        Instant earliestExpiry = null;

        for (CreateBookingResponse hold : holds) {
            Booking booking = bookingRepository.findByIdAndCustomerId(
                            UUID.fromString(hold.getBookingId()), customerId)
                    .orElseThrow();
            combinedSubtotal += booking.getRentalAmount() + booking.getDepositAmount() + booking.getDeliveryFee();
            bookings.add(booking);
            if (earliestExpiry == null || booking.getHoldExpiresAt().isBefore(earliestExpiry)) {
                earliestExpiry = booking.getHoldExpiresAt();
            }
        }

        long totalDiscount = 0;
        String couponCode = request.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            totalDiscount = couponService.validate(couponCode, combinedSubtotal).discountAmount();
        }

        distributeDiscount(bookings, combinedSubtotal, totalDiscount);

        UUID batchId = IdGenerator.uuidV7();
        CheckoutBatch batch = CheckoutBatch.builder()
                .id(batchId)
                .customerId(customerId)
                .deliveryAddressId(address.getId())
                .couponCode(couponCode)
                .discountAmount(totalDiscount)
                .totalAmount(combinedSubtotal - totalDiscount)
                .currencyCode(bookings.getFirst().getCurrencyCode())
                .status(CheckoutBatch.OPEN)
                .expiresAt(earliestExpiry)
                .idempotencyKey(idempotencyKey)
                .build();
        checkoutBatchRepository.save(batch);

        List<PrepareCheckoutBatchResponse.BookingHold> bookingHolds = new ArrayList<>();
        for (Booking booking : bookings) {
            booking.setCheckoutBatchId(batchId);
            bookingRepository.save(booking);

            if (booking.getCheckoutSessionId() != null) {
                CheckoutSession session = checkoutSessionRepository.findById(booking.getCheckoutSessionId())
                        .orElseThrow();
                session.setDeliveryAddressId(address.getId());
                session.setCouponCode(couponCode);
                session.setDiscountAmount(booking.getDiscountAmount());
                session.setReadyForPayment(true);
                session.setStatus(OPEN_CHECKOUT);
                checkoutSessionRepository.save(session);
            }

            bookingHolds.add(PrepareCheckoutBatchResponse.BookingHold.builder()
                    .bookingId(booking.getId().toString())
                    .rentalNumber(booking.getRentalNumber())
                    .checkoutSessionId(booking.getCheckoutSessionId().toString())
                    .totalAmount(booking.getTotalAmount())
                    .build());
        }

        return PrepareCheckoutBatchResponse.builder()
                .checkoutBatchId(batchId.toString())
                .totalAmount(batch.getTotalAmount())
                .discountAmount(totalDiscount)
                .currency(batch.getCurrencyCode())
                .holdExpiresAt(batch.getExpiresAt())
                .bookings(bookingHolds)
                .build();
    }

    private void distributeDiscount(List<Booking> bookings, long combinedSubtotal, long totalDiscount) {
        if (totalDiscount <= 0 || combinedSubtotal <= 0) {
            return;
        }

        long allocated = 0;
        for (int i = 0; i < bookings.size(); i++) {
            Booking booking = bookings.get(i);
            long itemSubtotal = booking.getRentalAmount() + booking.getDepositAmount() + booking.getDeliveryFee();
            long itemDiscount;
            if (i == bookings.size() - 1) {
                itemDiscount = totalDiscount - allocated;
            } else {
                itemDiscount = Math.round((double) totalDiscount * itemSubtotal / combinedSubtotal);
                allocated += itemDiscount;
            }
            booking.setDiscountAmount(itemDiscount);
            booking.setTotalAmount(itemSubtotal - itemDiscount);
            bookingRepository.save(booking);
        }
    }

    private boolean canReuseBatch(
            CheckoutBatch batch, UUID customerId, PrepareCheckoutBatchRequest request) {
        if (!batch.getCustomerId().equals(customerId)) {
            return false;
        }
        if (!CheckoutBatch.OPEN.equals(batch.getStatus())) {
            return false;
        }
        if (Instant.now().isAfter(batch.getExpiresAt())) {
            return false;
        }
        if (!request.getDeliveryAddressId().equals(batch.getDeliveryAddressId())) {
            return false;
        }
        List<Booking> bookings = bookingRepository.findByCheckoutBatchIdAndCustomerId(batch.getId(), customerId);
        if (bookings.stream().noneMatch(b -> BookingStatus.PENDING_PAYMENT.equals(b.getStatus()))) {
            return false;
        }
        return matchesBatchItems(bookings, request);
    }

    private boolean matchesBatchItems(List<Booking> bookings, PrepareCheckoutBatchRequest request) {
        List<Booking> pending = bookings.stream()
                .filter(b -> BookingStatus.PENDING_PAYMENT.equals(b.getStatus()))
                .toList();
        if (pending.size() != request.getItems().size()) {
            return false;
        }

        List<String> existingSignatures = new ArrayList<>();
        for (Booking booking : pending) {
            BookingItem item = bookingItemRepository.findByBookingId(booking.getId()).orElse(null);
            if (item == null) {
                return false;
            }
            existingSignatures.add(lineSignature(
                    item.getProductId(),
                    item.getProductVariantId(),
                    booking.getRentalStartDate(),
                    booking.getRentalEndDate()));
        }

        List<String> requestedSignatures = request.getItems().stream()
                .map(line -> lineSignature(
                        line.getProductId(),
                        line.getVariantId(),
                        line.getRentalStartDate(),
                        line.getRentalEndDate()))
                .toList();

        existingSignatures.sort(String::compareTo);
        ArrayList<String> sortedRequested = new ArrayList<>(requestedSignatures);
        sortedRequested.sort(String::compareTo);
        return existingSignatures.equals(sortedRequested);
    }

    private static String lineSignature(
            UUID productId, UUID variantId, java.time.LocalDate start, java.time.LocalDate end) {
        return productId + "|" + variantId + "|" + start + "|" + end;
    }

    private void releaseOtherOpenCheckouts(UUID customerId) {
        checkoutBatchRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(customerId, CheckoutBatch.OPEN)
                .forEach(this::abandonBatch);
    }

    private void abandonBatch(CheckoutBatch batch) {
        if (CheckoutBatch.COMPLETED.equals(batch.getStatus())) {
            return;
        }

        Instant now = Instant.now();
        List<Booking> bookings = bookingRepository.findByCheckoutBatchIdAndCustomerId(
                batch.getId(), batch.getCustomerId());
        for (Booking booking : bookings) {
            if (BookingStatus.PENDING_PAYMENT.equals(booking.getStatus())) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancelledAt(now);
                booking.setCancelReason("CHECKOUT_ABANDONED");
                booking.setIdempotencyKey(null);
                bookingRepository.save(booking);
                inventoryHoldService.releaseByBookingId(booking.getId(), InventoryHoldService.EXPIRED);
                timelineService.append(
                        booking.getId(),
                        null,
                        BookingStatus.CANCELLED,
                        "Checkout abandoned — hold released");
            }
        }

        if (CheckoutBatch.OPEN.equals(batch.getStatus())) {
            batch.setStatus(CheckoutBatch.EXPIRED);
        }
        batch.setIdempotencyKey(null);
        checkoutBatchRepository.save(batch);
    }

    private PrepareCheckoutBatchResponse rebuildResponse(CheckoutBatch batch, UUID customerId) {
        if (!batch.getCustomerId().equals(customerId)) {
            throw new ClosiqException(ErrorCode.NOT_FOUND, "Checkout batch not found");
        }
        if (CheckoutBatch.COMPLETED.equals(batch.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Checkout already completed");
        }

        List<Booking> bookings = bookingRepository.findByCheckoutBatchIdAndCustomerId(batch.getId(), customerId);
        if (bookings.isEmpty()) {
            throw new ClosiqException(ErrorCode.NOT_FOUND, "Checkout batch has no bookings");
        }

        List<PrepareCheckoutBatchResponse.BookingHold> holds = bookings.stream()
                .filter(b -> BookingStatus.PENDING_PAYMENT.equals(b.getStatus()))
                .map(b -> PrepareCheckoutBatchResponse.BookingHold.builder()
                        .bookingId(b.getId().toString())
                        .rentalNumber(b.getRentalNumber())
                        .checkoutSessionId(b.getCheckoutSessionId().toString())
                        .totalAmount(b.getTotalAmount())
                        .build())
                .toList();

        return PrepareCheckoutBatchResponse.builder()
                .checkoutBatchId(batch.getId().toString())
                .totalAmount(batch.getTotalAmount())
                .discountAmount(batch.getDiscountAmount())
                .currency(batch.getCurrencyCode())
                .holdExpiresAt(batch.getExpiresAt())
                .bookings(holds)
                .build();
    }
}
