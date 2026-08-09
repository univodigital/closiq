package com.closiq.seller.service;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingItem;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.repository.BookingItemRepository;
import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.service.BookingTimelineService;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.IdGenerator;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.config.ClosiqProperties;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.inventory.service.InventoryHoldService;
import com.closiq.payment.domain.Payment;
import com.closiq.payment.domain.PaymentStatus;
import com.closiq.payment.domain.Refund;
import com.closiq.payment.repository.PaymentRepository;
import com.closiq.payment.repository.RefundRepository;
import com.closiq.seller.domain.SellerRejectReason;
import com.closiq.seller.mapper.SellerBookingMapper;
import com.closiq.seller.web.dto.AcceptSellerBookingRequest;
import com.closiq.seller.web.dto.RejectSellerBookingRequest;
import com.closiq.seller.web.dto.SellerBookingDetailResponse;
import com.closiq.seller.web.dto.SellerBookingHistoryResponse;
import com.closiq.seller.web.dto.SellerBookingListItemResponse;
import com.closiq.shipment.service.ShipmentAccessService;
import com.closiq.user.domain.Address;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerBookingService {

    private static final Set<String> HISTORY_STATUSES = Set.of(
            BookingStatus.COMPLETED,
            BookingStatus.CANCELLED,
            BookingStatus.DEPOSIT_REFUNDED);

    private static final Set<String> ACTIVE_SELLER_STATUSES = Set.of(
            BookingStatus.CONFIRMED,
            BookingStatus.SELLER_ACCEPTED,
            BookingStatus.PREPARING,
            BookingStatus.OUT_FOR_DELIVERY,
            BookingStatus.TRIAL_READY,
            BookingStatus.RENTAL_ACTIVE,
            BookingStatus.RETURN_SCHEDULED,
            BookingStatus.RETURN_IN_TRANSIT,
            BookingStatus.RETURNED,
            BookingStatus.INSPECTION);

    private final SellerContextService sellerContextService;
    private final ShipmentAccessService shipmentAccessService;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BookingTimelineService timelineService;
    private final AddressRepository addressRepository;
    private final UserProfileRepository userProfileRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final InventoryHoldService inventoryHoldService;
    private final SellerBookingMapper sellerBookingMapper;
    private final ClosiqProperties properties;

    @Transactional(readOnly = true)
    public PagedResult<SellerBookingListItemResponse> listBookings(
            UUID userId,
            String status,
            UUID productId,
            LocalDate startDate,
            LocalDate endDate,
            String sort,
            String pageToken,
            Integer limit) {

        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        int pageSize = normalizeLimit(limit);
        boolean ascending = sort == null || sort.toLowerCase(Locale.ROOT).contains("asc");

        PageTokenCodec.SellerBookingPageToken token = PageTokenCodec.sellerBookingPageToken(pageToken, ascending);
        Specification<Booking> spec = sellerBookingSpec(
                seller.getId(), status, productId, startDate, endDate, token, ascending, false);

        Sort sortOrder = ascending
                ? Sort.by(Sort.Direction.ASC, "rentalStartDate", "id")
                : Sort.by(Sort.Direction.DESC, "rentalStartDate", "id");

        List<Booking> bookings = bookingRepository.findAll(
                spec, PageRequest.of(0, pageSize + 1, sortOrder)).getContent();

        return toListPage(bookings, pageSize, ascending);
    }

    @Transactional(readOnly = true)
    public SellerBookingDetailResponse getBooking(UUID userId, String bookingIdOrNumber) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Booking booking = shipmentAccessService.resolveSellerBooking(userId, bookingIdOrNumber);
        if (!seller.getId().equals(booking.getSellerProfileId())) {
            throw new ClosiqException(ErrorCode.FORBIDDEN);
        }

        BookingItem item = bookingItemRepository.findByBookingId(booking.getId()).orElseThrow();
        Address address = booking.getDeliveryAddressId() != null
                ? addressRepository.findById(booking.getDeliveryAddressId()).orElse(null)
                : null;
        UserProfile customer = userProfileRepository.findByUserId(booking.getCustomerId()).orElse(null);

        return sellerBookingMapper.toDetail(booking, item, address, customer, isCustomerVisible(booking), commissionRate());
    }

    @Transactional
    public Map<String, Object> acceptBooking(UUID userId, String bookingIdOrNumber, AcceptSellerBookingRequest request) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Booking booking = shipmentAccessService.resolveSellerBooking(userId, bookingIdOrNumber);

        if (!BookingStatus.CONFIRMED.equals(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        if (booking.getConfirmedAt() != null) {
            Instant deadline = booking.getConfirmedAt()
                    .plus(properties.getBooking().getSellerAcceptSlaHours(), ChronoUnit.HOURS);
            if (Instant.now().isAfter(deadline)) {
                throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Acceptance window has expired");
            }
        }

        Instant now = Instant.now();
        booking.setStatus(BookingStatus.SELLER_ACCEPTED);
        booking.setSellerAcceptedAt(now);
        booking.setSellerPrepBy(request.getEstimatedPrepBy());
        booking.setSellerNotes(request.getNotes());
        bookingRepository.save(booking);

        timelineService.append(
                booking.getId(),
                userId,
                BookingStatus.SELLER_ACCEPTED,
                "Seller accepted booking",
                request.getNotes());

        return Map.of(
                "status", BookingStatus.SELLER_ACCEPTED,
                "prepBy", request.getEstimatedPrepBy().toString());
    }

    @Transactional
    public Map<String, Object> rejectBooking(UUID userId, String bookingIdOrNumber, RejectSellerBookingRequest request) {
        validateRejectReason(request.getReason());

        Booking booking = shipmentAccessService.resolveSellerBooking(userId, bookingIdOrNumber);

        if (!BookingStatus.CONFIRMED.equals(booking.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        Instant now = Instant.now();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        booking.setCancelReason(request.getReason());
        booking.setCancelComment(request.getComment());
        bookingRepository.save(booking);

        inventoryHoldService.releaseByBookingId(booking.getId(), InventoryHoldService.RELEASED);
        initiateFullRefund(booking, userId, request.getReason());

        timelineService.append(
                booking.getId(),
                userId,
                BookingStatus.CANCELLED,
                "Seller rejected booking",
                request.getComment());

        return Map.of("status", BookingStatus.CANCELLED, "refundInitiated", true);
    }

    @Transactional(readOnly = true)
    public SellerBookingHistoryResponse getHistory(
            UUID userId,
            int page,
            int limit,
            LocalDate startDate,
            LocalDate endDate,
            List<String> statuses) {

        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(limit, 1), 50);

        List<String> statusFilter = resolveHistoryStatuses(statuses);
        Specification<Booking> spec = historySpec(seller.getId(), statusFilter, startDate, endDate);

        Page<Booking> result = bookingRepository.findAll(
                spec, PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<SellerBookingListItemResponse> items = result.getContent().stream()
                .map(booking -> {
                    BookingItem item = bookingItemRepository.findByBookingId(booking.getId()).orElseThrow();
                    Address address = booking.getDeliveryAddressId() != null
                            ? addressRepository.findById(booking.getDeliveryAddressId()).orElse(null)
                            : null;
                    UserProfile customer = userProfileRepository.findByUserId(booking.getCustomerId()).orElse(null);
                    return sellerBookingMapper.toListItem(
                            booking, item, address, customer, true, commissionRate());
                })
                .toList();

        long totalEarnings = items.stream().mapToLong(SellerBookingListItemResponse::getEarnings).sum();
        long totalCommission = items.stream().mapToLong(SellerBookingListItemResponse::getCommission).sum();

        return SellerBookingHistoryResponse.builder()
                .bookings(items)
                .summary(SellerBookingHistoryResponse.EarningsSummary.builder()
                        .totalEarnings(totalEarnings)
                        .totalCommission(totalCommission)
                        .currency("INR")
                        .build())
                .page(page)
                .limit(pageSize)
                .totalCount(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private void initiateFullRefund(Booking booking, UUID initiatedBy, String reason) {
        paymentRepository.findByBookingIdAndStatus(booking.getId(), PaymentStatus.CAPTURED).ifPresent(payment -> {
            Refund rentalRefund = Refund.builder()
                    .id(IdGenerator.uuidV7())
                    .paymentId(payment.getId())
                    .bookingId(booking.getId())
                    .initiatedBy(initiatedBy)
                    .refundType("FULL")
                    .amount(payment.getAmount())
                    .status("PENDING")
                    .reason(reason)
                    .initiatedAt(Instant.now())
                    .expectedBy(Instant.now().plus(5, ChronoUnit.DAYS))
                    .build();
            refundRepository.save(rentalRefund);
        });
    }

    private PagedResult<SellerBookingListItemResponse> toListPage(
            List<Booking> bookings, int pageSize, boolean ascending) {

        boolean hasMore = bookings.size() > pageSize;
        List<Booking> pageItems = hasMore ? bookings.subList(0, pageSize) : bookings;

        List<SellerBookingListItemResponse> items = pageItems.stream()
                .map(booking -> {
                    BookingItem item = bookingItemRepository.findByBookingId(booking.getId()).orElseThrow();
                    Address address = booking.getDeliveryAddressId() != null
                            ? addressRepository.findById(booking.getDeliveryAddressId()).orElse(null)
                            : null;
                    UserProfile customer = userProfileRepository.findByUserId(booking.getCustomerId()).orElse(null);
                    return sellerBookingMapper.toListItem(
                            booking, item, address, customer, isCustomerVisible(booking), commissionRate());
                })
                .toList();

        String nextPageToken = null;
        if (hasMore && !pageItems.isEmpty()) {
            Booking last = pageItems.get(pageItems.size() - 1);
            nextPageToken = PageTokenCodec.encodeSellerBooking(
                    new PageTokenCodec.SellerBookingPageToken(last.getRentalStartDate(), last.getId()));
        }

        return PagedResult.of(items, pageSize, hasMore, nextPageToken);
    }

    private Specification<Booking> sellerBookingSpec(
            UUID sellerProfileId,
            String status,
            UUID productId,
            LocalDate startDate,
            LocalDate endDate,
            PageTokenCodec.SellerBookingPageToken token,
            boolean ascending,
            boolean historyOnly) {

        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("sellerProfileId"), sellerProfileId));

            if (historyOnly) {
                predicates.add(root.get("status").in(HISTORY_STATUSES));
            } else {
                predicates.add(cb.not(root.get("status").in(
                        BookingStatus.PENDING_PAYMENT,
                        BookingStatus.CANCELLED,
                        BookingStatus.COMPLETED,
                        BookingStatus.DEPOSIT_REFUNDED)));
            }

            String mappedStatus = mapApiStatus(status);
            if (mappedStatus != null) {
                predicates.add(cb.equal(root.get("status"), mappedStatus));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rentalStartDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rentalStartDate"), endDate));
            }

            if (productId != null) {
                var sub = query.subquery(UUID.class);
                var itemRoot = sub.from(BookingItem.class);
                sub.select(itemRoot.get("bookingId"))
                        .where(cb.equal(itemRoot.get("productId"), productId));
                predicates.add(root.get("id").in(sub));
            }

            if (ascending) {
                predicates.add(cb.or(
                        cb.greaterThan(root.get("rentalStartDate"), token.rentalStartDate()),
                        cb.and(
                                cb.equal(root.get("rentalStartDate"), token.rentalStartDate()),
                                cb.greaterThan(root.get("id"), token.id()))));
            } else {
                predicates.add(cb.or(
                        cb.lessThan(root.get("rentalStartDate"), token.rentalStartDate()),
                        cb.and(
                                cb.equal(root.get("rentalStartDate"), token.rentalStartDate()),
                                cb.lessThan(root.get("id"), token.id()))));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Specification<Booking> historySpec(
            UUID sellerProfileId, List<String> statuses, LocalDate startDate, LocalDate endDate) {

        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("sellerProfileId"), sellerProfileId));
            predicates.add(root.get("status").in(statuses));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rentalStartDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rentalEndDate"), endDate));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private String mapApiStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if ("PENDING_ACCEPTANCE".equalsIgnoreCase(status)) {
            return BookingStatus.CONFIRMED;
        }
        return status.toUpperCase(Locale.ROOT);
    }

    private List<String> resolveHistoryStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.copyOf(HISTORY_STATUSES);
        }
        return statuses.stream().map(s -> s.toUpperCase(Locale.ROOT)).toList();
    }

    private void validateRejectReason(String reason) {
        Set<String> allowed = Set.of(
                SellerRejectReason.ITEM_DAMAGED,
                SellerRejectReason.ITEM_UNAVAILABLE,
                SellerRejectReason.UNABLE_TO_PREP,
                SellerRejectReason.OTHER);
        if (!allowed.contains(reason)) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Invalid reject reason");
        }
    }

    private boolean isCustomerVisible(Booking booking) {
        return !BookingStatus.CONFIRMED.equals(booking.getStatus());
    }

    private double commissionRate() {
        return properties.getBooking().getCommissionRateBps() / 10_000.0;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        return Math.min(Math.max(limit, 1), 50);
    }
}
