package com.closiq.seller.mapper;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingItem;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.identity.domain.UserProfile;
import com.closiq.seller.domain.SellerRejectReason;
import com.closiq.seller.repository.WalletTransactionRepository;
import com.closiq.seller.service.SellerAcceptanceService;
import com.closiq.seller.web.dto.RejectReasonOptionResponse;
import com.closiq.seller.web.dto.SellerBookingDetailResponse;
import com.closiq.seller.web.dto.SellerBookingListItemResponse;
import com.closiq.seller.web.dto.SellerRejectPreviewResponse;
import com.closiq.user.domain.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SellerBookingMapper {

    private final SellerAcceptanceService acceptanceService;
    private final WalletTransactionRepository walletTransactionRepository;

    public SellerBookingListItemResponse toListItem(
            Booking booking,
            BookingItem item,
            Address address,
            UserProfile customer,
            boolean showCustomer,
            double commissionRate,
            int refundExpectedBusinessDays) {

        Map<String, Object> snapshot = item.getPriceSnapshot();
        long commission = Math.round(booking.getRentalAmount() * commissionRate);
        long earnings = booking.getRentalAmount() - commission;

        if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
            commission = 0;
            earnings = 0;
        }

        Instant acceptDeadline = acceptanceService.isPendingAcceptance(booking)
                ? acceptanceService.acceptDeadline(booking)
                : null;

        return SellerBookingListItemResponse.builder()
                .id(booking.getId().toString())
                .rentalNumber(booking.getRentalNumber())
                .orderNumber(booking.getOrderNumber())
                .bookingId(booking.getRentalNumber())
                .orderId(booking.getOrderNumber())
                .productId(item.getProductId().toString())
                .productTitle(stringVal(snapshot, "productTitle"))
                .productImage(stringVal(snapshot, "imageUrl"))
                .customerName(showCustomer ? maskName(customer) : null)
                .variantSize(stringVal(snapshot, "variantLabel"))
                .status(booking.getStatus().toLowerCase(Locale.ROOT))
                .rentalStart(booking.getRentalStartDate())
                .rentalEnd(booking.getRentalEndDate())
                .rentalDays(booking.getRentalDays())
                .earnings(earnings)
                .commission(commission)
                .currency(booking.getCurrencyCode())
                .deliveryPincode(address != null ? address.getPincode() : null)
                .prepBy(booking.getSellerPrepBy())
                .notes(booking.getSellerNotes())
                .acceptDeadlineAt(acceptDeadline)
                .acceptanceExpired(acceptanceService.isAcceptanceExpired(booking))
                .refundExpectedBusinessDays(refundExpectedBusinessDays)
                .build();
    }

    public SellerBookingDetailResponse toDetail(
            Booking booking,
            BookingItem item,
            Address address,
            UserProfile customer,
            boolean showCustomer,
            double commissionRate,
            int refundExpectedBusinessDays) {

        Map<String, Object> snapshot = item.getPriceSnapshot();
        long commission = Math.round(booking.getRentalAmount() * commissionRate);
        long net = booking.getRentalAmount() - commission;
        boolean pendingAcceptance = acceptanceService.isPendingAcceptance(booking);
        boolean expired = acceptanceService.isAcceptanceExpired(booking);
        boolean canAccept = pendingAcceptance && !expired;
        boolean canReject = canAccept;
        boolean canMarkReady = BookingStatus.SELLER_ACCEPTED.equals(booking.getStatus());

        boolean credited = walletTransactionRepository.existsByReferenceTypeAndReferenceIdAndTxnType(
                "BOOKING", booking.getId().toString(), "CREDIT_EARNING");

        return SellerBookingDetailResponse.builder()
                .id(booking.getId().toString())
                .rentalNumber(booking.getRentalNumber())
                .orderNumber(booking.getOrderNumber())
                .bookingId(booking.getRentalNumber())
                .orderId(booking.getOrderNumber())
                .status(booking.getStatus())
                .productId(item.getProductId().toString())
                .productTitle(stringVal(snapshot, "productTitle"))
                .productImage(stringVal(snapshot, "imageUrl"))
                .variantSize(stringVal(snapshot, "variantLabel"))
                .rentalStart(booking.getRentalStartDate())
                .rentalEnd(booking.getRentalEndDate())
                .rentalDays(booking.getRentalDays())
                .currency(booking.getCurrencyCode())
                .earnings(SellerBookingDetailResponse.EarningsBreakdown.builder()
                        .rentalAmount(booking.getRentalAmount())
                        .commission(commission)
                        .netEarnings(net)
                        .depositHeld(booking.getDepositAmount())
                        .creditedToWallet(credited)
                        .build())
                .customer(SellerBookingDetailResponse.CustomerContact.builder()
                        .name(showCustomer ? maskName(customer) : null)
                        .phoneMasked(showCustomer ? "+91******" : null)
                        .deliveryPincode(address != null ? address.getPincode() : null)
                        .deliveryCity(address != null ? address.getCity() : null)
                        .build())
                .prepBy(booking.getSellerPrepBy())
                .notes(booking.getSellerNotes())
                .customerNotes(booking.getCustomerNotes())
                .prepChecklist(buildPrepChecklist(booking.getStatus()))
                .acceptDeadlineAt(pendingAcceptance ? acceptanceService.acceptDeadline(booking) : null)
                .acceptanceExpired(expired)
                .canAccept(canAccept)
                .canReject(canReject)
                .canMarkReady(canMarkReady)
                .acceptSlaHours(acceptanceService.acceptSlaHours())
                .refundExpectedBusinessDays(refundExpectedBusinessDays)
                .rejectReasons(rejectReasonOptions())
                .rejectPreview(canReject ? buildRejectPreview(booking, refundExpectedBusinessDays) : null)
                .build();
    }

    public SellerRejectPreviewResponse buildRejectPreview(Booking booking, int refundExpectedBusinessDays) {
        return SellerRejectPreviewResponse.builder()
                .refundAmount(booking.getTotalAmount())
                .expectedBusinessDays(refundExpectedBusinessDays)
                .refundMethod("Original payment method")
                .currency(booking.getCurrencyCode())
                .build();
    }

    private List<RejectReasonOptionResponse> rejectReasonOptions() {
        return List.of(
                option(SellerRejectReason.ITEM_UNAVAILABLE, "Item unavailable", false),
                option(SellerRejectReason.ITEM_DAMAGED, "Item damaged", false),
                option(SellerRejectReason.UNABLE_TO_PREP, "Unable to prepare in time", false),
                option(SellerRejectReason.OTHER, "Other", true));
    }

    private RejectReasonOptionResponse option(String code, String label, boolean requiresComment) {
        return RejectReasonOptionResponse.builder()
                .code(code)
                .label(label)
                .requiresComment(requiresComment)
                .build();
    }

    private List<SellerBookingDetailResponse.PrepChecklistItem> buildPrepChecklist(String status) {
        boolean accepted = !BookingStatus.CONFIRMED.equals(status);
        boolean preparing = Set.of(
                BookingStatus.PREPARING,
                BookingStatus.OUT_FOR_DELIVERY,
                BookingStatus.TRIAL_READY,
                BookingStatus.RENTAL_ACTIVE).contains(status);
        boolean shipped = Set.of(
                BookingStatus.OUT_FOR_DELIVERY,
                BookingStatus.TRIAL_READY,
                BookingStatus.RENTAL_ACTIVE).contains(status);

        return List.of(
                SellerBookingDetailResponse.PrepChecklistItem.builder()
                        .item("Accept booking")
                        .done(accepted)
                        .build(),
                SellerBookingDetailResponse.PrepChecklistItem.builder()
                        .item("Inspect and prepare garment")
                        .done(preparing)
                        .build(),
                SellerBookingDetailResponse.PrepChecklistItem.builder()
                        .item("Hand off to courier")
                        .done(shipped)
                        .build());
    }

    private String maskName(UserProfile profile) {
        if (profile == null) {
            return "Verified Customer";
        }
        if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
            return profile.getDisplayName();
        }
        String lastInitial = profile.getLastName() != null && !profile.getLastName().isBlank()
                ? profile.getLastName().substring(0, 1) + "."
                : "";
        return profile.getFirstName() + (lastInitial.isBlank() ? "" : " " + lastInitial);
    }

    private String stringVal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
