package com.closiq.booking.mapper;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingItem;
import com.closiq.booking.domain.BookingTimeline;
import com.closiq.booking.web.dto.BookingDetailResponse;
import com.closiq.booking.web.dto.BookingSummaryResponse;
import com.closiq.booking.web.dto.CreateBookingResponse;
import com.closiq.booking.web.dto.TimelineEventResponse;
import com.closiq.catalog.domain.Product;
import com.closiq.catalog.domain.ProductVariant;
import com.closiq.user.domain.Address;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class BookingMapper {

    public CreateBookingResponse toCreateResponse(
            Booking booking,
            Product product,
            ProductVariant variant,
            UUID checkoutSessionId) {

        String rentalNumber = booking.getRentalNumber();
        return CreateBookingResponse.builder()
                .bookingId(booking.getId().toString())
                .rentalNumber(rentalNumber)
                .bookingNumber(rentalNumber)
                .orderNumber(booking.getOrderNumber())
                .status(booking.getStatus())
                .holdExpiresAt(booking.getHoldExpiresAt())
                .checkoutSessionId(checkoutSessionId.toString())
                .product(CreateBookingResponse.ProductSnippet.builder()
                        .id(product.getId().toString())
                        .title(product.getTitle())
                        .variantSize(variant.getVariantLabel())
                        .build())
                .rentalStartDate(booking.getRentalStartDate())
                .rentalEndDate(booking.getRentalEndDate())
                .rentalDays(booking.getRentalDays())
                .includesTrial(booking.isIncludesTrial())
                .trialDurationMinutes(booking.getTrialDurationMinutes())
                .pricing(CreateBookingResponse.PricingBreakdown.builder()
                        .rentalAmount(booking.getRentalAmount())
                        .depositAmount(booking.getDepositAmount())
                        .deliveryFee(booking.getDeliveryFee())
                        .discountAmount(booking.getDiscountAmount())
                        .totalAmount(booking.getTotalAmount())
                        .currency(booking.getCurrencyCode())
                        .build())
                .build();
    }

    public BookingSummaryResponse toSummary(Booking booking, BookingItem item) {
        Map<String, Object> snapshot = item.getPriceSnapshot();
        String rentalNumber = booking.getRentalNumber();
        return BookingSummaryResponse.builder()
                .id(booking.getId().toString())
                .rentalNumber(rentalNumber)
                .bookingNumber(rentalNumber)
                .orderNumber(booking.getOrderNumber())
                .status(booking.getStatus())
                .productTitle(stringVal(snapshot, "productTitle"))
                .productImage(stringVal(snapshot, "imageUrl"))
                .variantSize(stringVal(snapshot, "variantLabel"))
                .rentalStartDate(booking.getRentalStartDate())
                .rentalEndDate(booking.getRentalEndDate())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrencyCode())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    public BookingDetailResponse toDetail(
            Booking booking,
            BookingItem item,
            Address address,
            List<BookingTimeline> timeline) {

        Map<String, Object> snapshot = item.getPriceSnapshot();
        String currentStatus = booking.getStatus();
        String rentalNumber = booking.getRentalNumber();

        return BookingDetailResponse.builder()
                .id(booking.getId().toString())
                .rentalNumber(rentalNumber)
                .bookingNumber(rentalNumber)
                .orderNumber(booking.getOrderNumber())
                .status(booking.getStatus())
                .productId(item.getProductId().toString())
                .productTitle(stringVal(snapshot, "productTitle"))
                .productImage(stringVal(snapshot, "imageUrl"))
                .variantSize(stringVal(snapshot, "variantLabel"))
                .rentalStartDate(booking.getRentalStartDate())
                .rentalEndDate(booking.getRentalEndDate())
                .rentalDays(booking.getRentalDays())
                .rentalAmount(booking.getRentalAmount())
                .depositAmount(booking.getDepositAmount())
                .deliveryFee(booking.getDeliveryFee())
                .discountAmount(booking.getDiscountAmount())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrencyCode())
                .includesTrial(booking.isIncludesTrial())
                .trialDurationMinutes(booking.getTrialDurationMinutes())
                .holdExpiresAt(booking.getHoldExpiresAt())
                .createdAt(booking.getCreatedAt())
                .deliveryAddress(address != null
                        ? BookingDetailResponse.DeliveryAddress.builder()
                                .line1(address.getLine1())
                                .line2(address.getLine2())
                                .city(address.getCity())
                                .state(address.getState())
                                .pincode(address.getPincode())
                                .build()
                        : null)
                .refundDetails(buildRefundDetails(booking))
                .timeline(timeline.stream().map(t -> toTimelineEvent(t, currentStatus)).toList())
                .build();
    }

    public TimelineEventResponse toTimelineEvent(BookingTimeline event, String currentStatus) {
        boolean isCurrent = event.getStatus().equals(currentStatus);
        return TimelineEventResponse.builder()
                .status(event.getStatus())
                .label(event.getLabel())
                .timestamp(event.getOccurredAt())
                .completed(!isCurrent)
                .current(isCurrent)
                .pending(false)
                .build();
    }

    private BookingDetailResponse.RefundDetails buildRefundDetails(Booking booking) {
        if (!"CANCELLED".equals(booking.getStatus()) && !"REFUND_PENDING".equals(booking.getStatus())
                && !"TRIAL_REJECTED".equals(booking.getStatus())) {
            return null;
        }
        return BookingDetailResponse.RefundDetails.builder()
                .refundAmount(booking.getRentalAmount())
                .depositRefundAmount(booking.getDepositAmount())
                .status("REFUND_PENDING".equals(booking.getStatus()) ? "REFUND_PENDING" : "PENDING")
                .build();
    }

    private String stringVal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
