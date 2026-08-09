package com.closiq.booking.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingItem;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.domain.BookingTimeline;
import com.closiq.booking.domain.CheckoutSession;
import com.closiq.booking.mapper.BookingMapper;
import com.closiq.booking.repository.BookingItemRepository;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.repository.BookingTimelineRepository;
import com.closiq.booking.repository.CheckoutSessionRepository;
import com.closiq.booking.web.dto.BookingDetailResponse;
import com.closiq.booking.web.dto.BookingSummaryResponse;
import com.closiq.booking.web.dto.CancelBookingRequest;
import com.closiq.booking.web.dto.CreateBookingRequest;
import com.closiq.booking.web.dto.CreateBookingResponse;
import com.closiq.booking.web.dto.TimelineEventResponse;
import com.closiq.catalog.domain.Product;
import com.closiq.catalog.domain.ProductVariant;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.catalog.repository.ProductVariantRepository;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.identifier.OrderNumberGenerator;
import com.closiq.common.identifier.RentalNumberGenerator;
import com.closiq.common.util.IdGenerator;
import com.closiq.common.web.PageBoundary;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.config.ClosiqProperties;
import com.closiq.inventory.domain.InventoryItem;
import com.closiq.inventory.service.AvailabilityService;
import com.closiq.inventory.service.InventoryHoldService;
import com.closiq.user.domain.Address;
import com.closiq.user.repository.AddressRepository;
import com.closiq.user.repository.ServiceablePincodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final String ACTIVE_PRODUCT = "ACTIVE";
    private static final String ACTIVE_VARIANT = "ACTIVE";
    private static final String ACTIVE_PINCODE = "ACTIVE";
    private static final String OPEN_CHECKOUT = "OPEN";

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BookingTimelineRepository timelineRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final AddressRepository addressRepository;
    private final ServiceablePincodeRepository serviceablePincodeRepository;
    private final AvailabilityService availabilityService;
    private final InventoryHoldService inventoryHoldService;
    private final BookingLockService bookingLockService;
    private final BookingPricingService pricingService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final RentalNumberGenerator rentalNumberGenerator;
    private final BookingTimelineService timelineService;
    private final BookingMapper bookingMapper;
    private final BookingHoldExpiryService holdExpiryService;
    private final ClosiqProperties properties;

    @Transactional
    public CreateBookingResponse createHold(UUID customerId, String idempotencyKey, CreateBookingRequest request) {
        holdExpiryService.releaseExpiredHolds();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Booking booking = existing.get();
                if (!matchesRequest(booking, request)) {
                    throw new ClosiqException(ErrorCode.IDEMPOTENCY_CONFLICT);
                }
                return rebuildCreateResponse(booking);
            }
        }

        validateDates(request.getRentalStartDate(), request.getRentalEndDate());

        Product product = productRepository.findByIdAndDeletedAtIsNullAndStatus(request.getProductId(), ACTIVE_PRODUCT)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Product not found"));

        ProductVariant variant = productVariantRepository.findByIdAndProductId(request.getVariantId(), product.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Variant not found"));

        if (!ACTIVE_VARIANT.equals(variant.getStatus())) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Variant is not available");
        }

        Address address = validateOptionalAddress(customerId, request.getDeliveryAddressId());

        long rentalDays = ChronoUnit.DAYS.between(request.getRentalStartDate(), request.getRentalEndDate()) + 1;
        if (rentalDays < product.getMinRentalDays()) {
            throw new ClosiqException(ErrorCode.MIN_RENTAL_PERIOD);
        }
        if (product.getMaxRentalDays() != null && rentalDays > product.getMaxRentalDays()) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Rental exceeds maximum allowed days");
        }

        LocalDate effectiveEnd = request.getRentalEndDate().plusDays(product.getCleaningBufferDays());

        if (!bookingLockService.tryAcquireVariantDateLock(
                variant.getId(), request.getRentalStartDate(), effectiveEnd)) {
            throw new ClosiqException(ErrorCode.BOOKING_CONFLICT);
        }

        try {
            InventoryItem inventoryItem = availabilityService
                    .selectAvailableItem(variant.getId(), request.getRentalStartDate(), effectiveEnd)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.BOOKING_CONFLICT));

            BookingPricingService.PricingBreakdown pricing = pricingService.calculate(
                    product, request.getRentalStartDate(), request.getRentalEndDate());

            Instant holdExpiresAt = Instant.now().plusSeconds(properties.getBooking().getHoldTtlMinutes() * 60L);
            UUID bookingId = IdGenerator.uuidV7();
            UUID checkoutSessionId = IdGenerator.uuidV7();

            CheckoutSession checkoutSession = CheckoutSession.builder()
                    .id(checkoutSessionId)
                    .customerId(customerId)
                    .status(OPEN_CHECKOUT)
                    .expiresAt(holdExpiresAt)
                    .build();
            checkoutSessionRepository.save(checkoutSession);

            Booking booking = Booking.builder()
                    .id(bookingId)
                    .rentalNumber(rentalNumberGenerator.nextCode())
                    .orderNumber(orderNumberGenerator.nextCode())
                    .customerId(customerId)
                    .sellerProfileId(product.getSellerProfileId())
                    .deliveryAddressId(address != null ? address.getId() : null)
                    .checkoutSessionId(checkoutSessionId)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .rentalStartDate(request.getRentalStartDate())
                    .rentalEndDate(request.getRentalEndDate())
                    .rentalDays(pricing.getRentalDays())
                    .rentalAmount(pricing.getRentalAmount())
                    .depositAmount(pricing.getDepositAmount())
                    .discountAmount(pricing.getDiscountAmount())
                    .deliveryFee(pricing.getDeliveryFee())
                    .totalAmount(pricing.getTotalAmount())
                    .currencyCode(pricing.getCurrency())
                    .includesTrial(product.isIncludesTrial())
                    .trialDurationMinutes(product.getTrialDurationMinutes())
                    .customerNotes(request.getCustomerNotes())
                    .idempotencyKey(idempotencyKey)
                    .holdExpiresAt(holdExpiresAt)
                    .build();
            bookingRepository.save(booking);

            checkoutSession.setBookingId(bookingId);
            checkoutSessionRepository.save(checkoutSession);

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("pricePerDay", product.getPricePerDay());
            snapshot.put("deposit", product.getDepositAmount());
            snapshot.put("productTitle", product.getTitle());
            snapshot.put("variantLabel", variant.getVariantLabel());
            snapshot.put("imageUrl", product.getPrimaryImageUrl());

            bookingItemRepository.save(BookingItem.builder()
                    .id(IdGenerator.uuidV7())
                    .bookingId(bookingId)
                    .productId(product.getId())
                    .productVariantId(variant.getId())
                    .inventoryItemId(inventoryItem.getId())
                    .priceSnapshot(snapshot)
                    .quantity((short) 1)
                    .build());

            inventoryHoldService.createHold(
                    inventoryItem, bookingId, request.getRentalStartDate(), effectiveEnd, holdExpiresAt);

            timelineService.append(
                    bookingId,
                    customerId,
                    BookingStatus.PENDING_PAYMENT,
                    "Booking hold created — complete payment within "
                            + properties.getBooking().getHoldTtlMinutes() + " minutes");

            return bookingMapper.toCreateResponse(booking, product, variant, checkoutSessionId);
        } finally {
            bookingLockService.releaseVariantDateLock(
                    variant.getId(), request.getRentalStartDate(), effectiveEnd);
        }
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse getBooking(UUID customerId, String bookingIdOrNumber) {
        Booking booking = resolveBooking(customerId, bookingIdOrNumber);
        BookingItem item = bookingItemRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking item not found"));
        Address address = booking.getDeliveryAddressId() != null
                ? addressRepository.findById(booking.getDeliveryAddressId()).orElse(null)
                : null;
        List<BookingTimeline> timeline = timelineRepository.findByBookingIdOrderByOccurredAtAsc(booking.getId());
        return bookingMapper.toDetail(booking, item, address, timeline);
    }

    @Transactional(readOnly = true)
    public PagedResult<BookingSummaryResponse> listBookings(
            UUID customerId, String status, String pageToken, Integer limit) {

        int pageSize = normalizeLimit(limit);
        PageBoundary boundary = PageTokenCodec.bookingBoundary(pageToken);

        List<Booking> bookings = bookingRepository.findCustomerPage(
                customerId,
                status,
                boundary.beforeCreatedAt(),
                boundary.beforeId(),
                PageRequest.of(0, pageSize + 1));

        boolean hasMore = bookings.size() > pageSize;
        List<Booking> pageItems = hasMore ? bookings.subList(0, pageSize) : bookings;

        List<BookingSummaryResponse> responses = pageItems.stream()
                .map(booking -> {
                    BookingItem item = bookingItemRepository.findByBookingId(booking.getId()).orElseThrow();
                    return bookingMapper.toSummary(booking, item);
                })
                .toList();

        String nextPageToken = hasMore && !pageItems.isEmpty()
                ? PageTokenCodec.encodeBooking(new PageTokenCodec.BookingPageToken(
                        pageItems.getLast().getCreatedAt(),
                        pageItems.getLast().getId()))
                : null;

        return PagedResult.of(responses, pageSize, hasMore, nextPageToken);
    }

    @Transactional
    public BookingDetailResponse cancelBooking(UUID customerId, String bookingIdOrNumber, CancelBookingRequest request) {
        Booking booking = resolveBooking(customerId, bookingIdOrNumber);

        if (!BookingStatus.PENDING_PAYMENT.equals(booking.getStatus())
                && !BookingStatus.CONFIRMED.equals(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        Instant now = Instant.now();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        booking.setCancelReason(request.getReason());
        booking.setCancelComment(request.getComment());
        bookingRepository.save(booking);

        inventoryHoldService.releaseByBookingId(booking.getId(), InventoryHoldService.RELEASED);
        timelineService.append(booking.getId(), customerId, BookingStatus.CANCELLED, "Booking cancelled");

        return getBooking(customerId, booking.getId().toString());
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> getTimeline(UUID customerId, String bookingIdOrNumber) {
        Booking booking = resolveBooking(customerId, bookingIdOrNumber);
        return timelineRepository.findByBookingIdOrderByOccurredAtAsc(booking.getId()).stream()
                .map(t -> bookingMapper.toTimelineEvent(t, booking.getStatus()))
                .toList();
    }

    private Booking resolveBooking(UUID customerId, String bookingIdOrNumber) {
        if (bookingIdOrNumber.startsWith("VST-RNT-") || bookingIdOrNumber.startsWith("BK-")) {
            return bookingRepository.findByRentalNumberAndCustomerId(bookingIdOrNumber, customerId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        }
        if (bookingIdOrNumber.startsWith("VST-ORD-")) {
            return bookingRepository.findByOrderNumberAndCustomerId(bookingIdOrNumber, customerId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        }
        try {
            UUID id = UUID.fromString(bookingIdOrNumber);
            return bookingRepository.findByIdAndCustomerId(id, customerId)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found"));
        } catch (IllegalArgumentException ex) {
            throw new ClosiqException(ErrorCode.NOT_FOUND, "Booking not found");
        }
    }

    private void validateDates(LocalDate start, LocalDate end) {
        LocalDate earliest = LocalDate.now().plusDays(properties.getBooking().getRentalLeadDays());
        if (start.isBefore(earliest)) {
            throw new ClosiqException(
                    ErrorCode.VALIDATION_ERROR,
                    "Rental must start at least " + properties.getBooking().getRentalLeadDays() + " days from today");
        }
        if (end.isBefore(start)) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Rental end date must be on or after start date");
        }
    }

    private Address validateOptionalAddress(UUID customerId, UUID addressId) {
        if (addressId == null) {
            return null;
        }
        Address address = addressRepository.findByIdAndUserIdAndDeletedAtIsNull(addressId, customerId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Address not found"));
        if (!serviceablePincodeRepository.findByPincodeAndStatus(address.getPincode(), ACTIVE_PINCODE).isPresent()) {
            throw new ClosiqException(ErrorCode.PINCODE_NOT_SERVICEABLE);
        }
        return address;
    }

    private boolean matchesRequest(Booking booking, CreateBookingRequest request) {
        BookingItem item = bookingItemRepository.findByBookingId(booking.getId()).orElse(null);
        if (item == null) {
            return false;
        }
        return item.getProductId().equals(request.getProductId())
                && item.getProductVariantId().equals(request.getVariantId())
                && booking.getRentalStartDate().equals(request.getRentalStartDate())
                && booking.getRentalEndDate().equals(request.getRentalEndDate())
                && java.util.Objects.equals(booking.getDeliveryAddressId(), request.getDeliveryAddressId());
    }

    private CreateBookingResponse rebuildCreateResponse(Booking booking) {
        BookingItem item = bookingItemRepository.findByBookingId(booking.getId()).orElseThrow();
        Product product = productRepository.findById(item.getProductId()).orElseThrow();
        ProductVariant variant = productVariantRepository.findById(item.getProductVariantId()).orElseThrow();
        return bookingMapper.toCreateResponse(booking, product, variant, booking.getCheckoutSessionId());
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 20;
        }
        return Math.min(limit, 50);
    }
}
