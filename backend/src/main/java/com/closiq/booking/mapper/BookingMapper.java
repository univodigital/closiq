package com.closiq.booking.mapper;

import com.closiq.booking.domain.Booking;
import com.closiq.booking.domain.BookingItem;
import com.closiq.booking.domain.BookingStatus;
import com.closiq.booking.domain.BookingTimeline;
import com.closiq.booking.service.BookingInvoiceService;
import com.closiq.booking.service.BookingLifecycleTimelineBuilder;
import com.closiq.booking.service.CancellationPolicyService;
import com.closiq.booking.service.TrialPolicyService;
import com.closiq.booking.web.dto.BookingDetailResponse;
import com.closiq.booking.web.dto.BookingSummaryResponse;
import com.closiq.booking.domain.TrialSession;
import com.closiq.shipment.domain.Shipment;
import com.closiq.shipment.domain.ShipmentStatus;
import com.closiq.booking.web.dto.CancelPreviewResponse;
import com.closiq.booking.web.dto.TrialRejectPreviewResponse;
import com.closiq.booking.web.dto.CreateBookingResponse;
import com.closiq.booking.web.dto.TimelineEventResponse;
import com.closiq.catalog.domain.Product;
import com.closiq.catalog.domain.ProductVariant;
import com.closiq.config.ClosiqProperties;
import com.closiq.payment.domain.Payment;
import com.closiq.payment.domain.Refund;
import com.closiq.user.domain.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingMapper {

    private final BookingLifecycleTimelineBuilder timelineBuilder;
    private final CancellationPolicyService cancellationPolicyService;
    private final TrialPolicyService trialPolicyService;
    private final BookingInvoiceService bookingInvoiceService;
    private final ClosiqProperties properties;

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

    public BookingSummaryResponse toSummary(Booking booking, BookingItem item, Payment payment) {
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
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .paymentPending(BookingStatus.PENDING_PAYMENT.equals(booking.getStatus()))
                .checkoutBatchId(booking.getCheckoutBatchId() != null
                        ? booking.getCheckoutBatchId().toString()
                        : null)
                .build();
    }

    public BookingSummaryResponse toSummary(Booking booking, BookingItem item) {
        return toSummary(booking, item, null);
    }

    public BookingDetailResponse toDetail(
            Booking booking,
            BookingItem item,
            Address address,
            List<BookingTimeline> history,
            Payment payment,
            List<Refund> refunds,
            TrialSession trialSession,
            Shipment returnShipment) {

        Map<String, Object> snapshot = item.getPriceSnapshot();
        String rentalNumber = booking.getRentalNumber();
        List<TimelineEventResponse> timeline = timelineBuilder.build(booking, history);
        CancelPreviewResponse cancelPreview = cancellationPolicyService.preview(booking);
        TrialRejectPreviewResponse trialRejectPreview = trialPolicyService.previewReject(booking);
        ClosiqProperties.Cancellation cancelConfig = properties.getBooking().getCancellation();

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
                .trialInfo(buildTrialInfo(trialSession))
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
                .paymentSummary(buildPaymentSummary(booking, payment))
                .refundDetails(buildRefundDetails(booking, refunds, cancelPreview, trialRejectPreview))
                .depositSummary(buildDepositSummary(booking, refunds, cancelConfig))
                .returnPickup(buildReturnPickup(booking, returnShipment))
                .cancellation(BookingDetailResponse.CancellationInfo.builder()
                        .eligible(cancelPreview.isEligible())
                        .policyLabel(cancelPreview.getPolicyLabel())
                        .build())
                .invoiceAvailable(bookingInvoiceService.isInvoiceAvailable(booking))
                .depositRefundExpectedBusinessDays(cancelConfig.getDepositRefundDaysMax())
                .timeline(timeline)
                .build();
    }

    public TimelineEventResponse toTimelineEvent(BookingTimeline event, String currentStatus) {
        boolean isCurrent = event.getStatus().equals(currentStatus);
        return TimelineEventResponse.builder()
                .status(event.getStatus())
                .label(event.getLabel())
                .description(event.getDescription())
                .timestamp(event.getOccurredAt())
                .completed(!isCurrent)
                .current(isCurrent)
                .pending(false)
                .build();
    }

    private BookingDetailResponse.PaymentSummary buildPaymentSummary(Booking booking, Payment payment) {
        if (BookingStatus.PENDING_PAYMENT.equals(booking.getStatus())) {
            return BookingDetailResponse.PaymentSummary.builder()
                    .status("PENDING")
                    .rentalAmount(booking.getRentalAmount())
                    .depositAmount(booking.getDepositAmount())
                    .deliveryFee(booking.getDeliveryFee())
                    .discountAmount(booking.getDiscountAmount())
                    .totalPaid(booking.getTotalAmount())
                    .checkoutBatchId(booking.getCheckoutBatchId() != null
                            ? booking.getCheckoutBatchId().toString()
                            : null)
                    .paymentPending(true)
                    .build();
        }
        if (payment == null) {
            return BookingDetailResponse.PaymentSummary.builder()
                    .status("PAID")
                    .rentalAmount(booking.getRentalAmount())
                    .depositAmount(booking.getDepositAmount())
                    .deliveryFee(booking.getDeliveryFee())
                    .discountAmount(booking.getDiscountAmount())
                    .totalPaid(booking.getTotalAmount())
                    .paymentPending(false)
                    .build();
        }
        return BookingDetailResponse.PaymentSummary.builder()
                .paymentId(payment.getId().toString())
                .status(payment.getStatus())
                .method(payment.getPaymentMethod())
                .rentalAmount(payment.getRentalComponent() / 100)
                .depositAmount(payment.getDepositComponent() / 100)
                .deliveryFee(booking.getDeliveryFee())
                .discountAmount(payment.getDiscountComponent() / 100)
                .totalPaid(payment.getAmount() / 100)
                .paidAt(payment.getCapturedAt())
                .checkoutBatchId(booking.getCheckoutBatchId() != null
                        ? booking.getCheckoutBatchId().toString()
                        : null)
                .paymentPending(false)
                .build();
    }

    private BookingDetailResponse.RefundDetails buildRefundDetails(
            Booking booking,
            List<Refund> refunds,
            CancelPreviewResponse cancelPreview,
            TrialRejectPreviewResponse trialRejectPreview) {

        if (BookingStatus.CANCELLED.equals(booking.getStatus())
                || BookingStatus.REFUND_PENDING.equals(booking.getStatus())
                || BookingStatus.TRIAL_REJECTED.equals(booking.getStatus())) {

            if (refunds != null && !refunds.isEmpty()) {
                return mapRefundDetails(refunds, cancelPreview, trialRejectPreview, booking);
            }

            if (BookingStatus.TRIAL_REJECTED.equals(booking.getStatus())) {
                return BookingDetailResponse.RefundDetails.builder()
                        .refundAmount(trialRejectPreview.getRentalRefundAmount())
                        .depositRefundAmount(trialRejectPreview.getDepositRefundAmount())
                        .totalRefunded(0)
                        .status("PENDING")
                        .refundMethod(trialRejectPreview.getRefundMethod())
                        .expectedBusinessDays(trialRejectPreview.getRentalRefundExpectedBusinessDays())
                        .items(List.of())
                        .build();
            }

            return BookingDetailResponse.RefundDetails.builder()
                    .refundAmount(cancelPreview.getRefundAmount())
                    .depositRefundAmount(cancelPreview.getDepositRefundAmount())
                    .totalRefunded(0)
                    .status("PENDING")
                    .refundMethod("ORIGINAL_PAYMENT_METHOD")
                    .expectedBusinessDays(cancelPreview.getExpectedRefundBusinessDays())
                    .items(List.of())
                    .build();
        }
        return refunds != null && !refunds.isEmpty()
                ? mapRefundDetails(refunds, cancelPreview, trialRejectPreview, booking)
                : null;
    }

    private BookingDetailResponse.RefundDetails mapRefundDetails(
            List<Refund> refunds,
            CancelPreviewResponse cancelPreview,
            TrialRejectPreviewResponse trialRejectPreview,
            Booking booking) {

        long totalRefunded = refunds.stream()
                .filter(r -> "PROCESSED".equals(r.getStatus()))
                .mapToLong(Refund::getAmount)
                .sum() / 100;
        long depositRefunded = refunds.stream()
                .filter(r -> "DEPOSIT".equals(r.getRefundType()) && !"FAILED".equals(r.getStatus()))
                .mapToLong(Refund::getAmount)
                .sum() / 100;

        String aggregateStatus = refunds.stream()
                .map(Refund::getStatus)
                .filter(s -> !"PROCESSED".equals(s))
                .findFirst()
                .orElse("PROCESSED");

        Instant expectedBy = refunds.stream()
                .map(Refund::getExpectedBy)
                .filter(e -> e != null)
                .max(Instant::compareTo)
                .orElse(null);

        List<BookingDetailResponse.RefundItem> items = refunds.stream()
                .map(r -> BookingDetailResponse.RefundItem.builder()
                        .refundId(r.getId().toString())
                        .type(r.getRefundType())
                        .amount(r.getAmount() / 100)
                        .status(r.getStatus())
                        .initiatedAt(r.getInitiatedAt())
                        .processedAt(r.getProcessedAt())
                        .expectedBy(r.getExpectedBy())
                        .build())
                .toList();

        return BookingDetailResponse.RefundDetails.builder()
                .refundAmount(totalRefunded > 0 ? totalRefunded : items.stream().mapToLong(i -> i.getAmount()).sum())
                .depositRefundAmount(depositRefunded)
                .totalRefunded(totalRefunded)
                .status(aggregateStatus)
                .refundMethod("ORIGINAL_PAYMENT_METHOD")
                .expectedBusinessDays(BookingStatus.TRIAL_REJECTED.equals(booking.getStatus())
                        ? trialRejectPreview.getRentalRefundExpectedBusinessDays()
                        : cancelPreview.getExpectedRefundBusinessDays())
                .expectedBy(expectedBy)
                .items(items)
                .build();
    }

    private BookingDetailResponse.DepositSummary buildDepositSummary(
            Booking booking, List<Refund> refunds, ClosiqProperties.Cancellation cancelConfig) {

        String status = booking.getStatus();
        if (BookingStatus.PENDING_PAYMENT.equals(status) || BookingStatus.CANCELLED.equals(status)) {
            return null;
        }

        long damage = booking.getInspectionDamageDeduction();
        long late = booking.getInspectionLateFee();
        long cleaning = booking.getInspectionCleaningFee();
        long totalDeduction = damage + late + cleaning;

        String depositStatus = "HELD";
        String inspectionStatus = null;
        long refundAmount = 0;
        String refundStatus = null;
        String deductionReason = null;

        if (BookingStatus.RETURNED.equals(status) || BookingStatus.INSPECTION.equals(status)) {
            depositStatus = "INSPECTION_PENDING";
            inspectionStatus = "IN_PROGRESS";
        } else if (BookingStatus.DEPOSIT_REFUNDED.equals(status) || BookingStatus.COMPLETED.equals(status)) {
            depositStatus = totalDeduction >= booking.getDepositAmount() ? "WITHHELD" : "REFUND_PROCESSING";
            inspectionStatus = "COMPLETED";
            Refund depositRefund = refunds != null
                    ? refunds.stream().filter(r -> "DEPOSIT".equals(r.getRefundType())).findFirst().orElse(null)
                    : null;
            if (depositRefund != null) {
                refundAmount = depositRefund.getAmount() / 100;
                refundStatus = depositRefund.getStatus();
            } else if (totalDeduction < booking.getDepositAmount()) {
                refundAmount = Math.max(0, booking.getDepositAmount() - totalDeduction);
                refundStatus = "PROCESSING";
            }
            deductionReason = buildDeductionReason(booking, damage, late, cleaning);
        } else if (BookingStatus.TRIAL_REJECTED.equals(status)
                || BookingStatus.TRIAL_READY.equals(status)
                || BookingStatus.RENTAL_ACTIVE.equals(status)
                || BookingStatus.RETURN_SCHEDULED.equals(status)
                || BookingStatus.RETURN_IN_TRANSIT.equals(status)) {
            depositStatus = "HELD";
        }

        int slaMin = cancelConfig.getDepositRefundDaysMin();
        int slaMax = cancelConfig.getDepositRefundDaysMax();
        String expectedWindow = slaMin + "–" + slaMax + " business days after inspection";

        return BookingDetailResponse.DepositSummary.builder()
                .depositStatus(depositStatus)
                .inspectionStatus(inspectionStatus)
                .depositAmount(booking.getDepositAmount())
                .damageDeduction(damage)
                .lateFee(late)
                .cleaningFee(cleaning)
                .totalDeduction(totalDeduction)
                .deductionReason(deductionReason)
                .refundAmount(refundAmount)
                .refundStatus(refundStatus)
                .refundMethod("ORIGINAL_PAYMENT_METHOD")
                .expectedRefundWindow(expectedWindow)
                .build();
    }

    private BookingDetailResponse.ReturnPickupSummary buildReturnPickup(Booking booking, Shipment returnShipment) {
        if (returnShipment == null) {
            return null;
        }
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate pickupDate = returnShipment.getPickupScheduledAt() != null
                ? returnShipment.getPickupScheduledAt().atZone(zone).toLocalDate()
                : null;

        return BookingDetailResponse.ReturnPickupSummary.builder()
                .shipmentId(returnShipment.getId())
                .returnReference(returnShipment.getTrackingNumber())
                .status(returnShipment.getStatus())
                .pickupDate(pickupDate)
                .pickupWindow(formatPickupWindow(returnShipment.getPickupTimeSlot()))
                .pickupScheduledAt(returnShipment.getPickupScheduledAt())
                .pickedUpAt(returnShipment.getPickedUpAt())
                .completedAt(ShipmentStatus.DELIVERED.equals(returnShipment.getStatus())
                        ? returnShipment.getDeliveredAt()
                        : null)
                .agentName(returnShipment.getAgentName())
                .build();
    }

    private String formatPickupWindow(String slot) {
        if (slot == null || slot.isBlank()) {
            return null;
        }
        return slot.replace("-", " – ");
    }

    private String buildDeductionReason(Booking booking, long damage, long late, long cleaning) {
        if (damage == 0 && late == 0 && cleaning == 0) {
            return booking.getInspectionNotes();
        }
        List<String> parts = new java.util.ArrayList<>();
        if (damage > 0) {
            parts.add("Damage found during inspection");
        }
        if (late > 0) {
            parts.add("Late return fee");
        }
        if (cleaning > 0) {
            parts.add("Cleaning fee");
        }
        if (booking.getInspectionNotes() != null && !booking.getInspectionNotes().isBlank()) {
            parts.add(booking.getInspectionNotes().trim());
        }
        return String.join("; ", parts);
    }

    private BookingDetailResponse.TrialInfo buildTrialInfo(TrialSession trialSession) {
        if (trialSession == null) {
            return null;
        }
        Instant now = Instant.now();
        boolean expired = "EXPIRED".equals(trialSession.getOutcome())
                || ("PENDING".equals(trialSession.getOutcome())
                        && trialSession.getExpiresAt() != null
                        && now.isAfter(trialSession.getExpiresAt()));
        boolean active = "PENDING".equals(trialSession.getOutcome()) && !expired;

        return BookingDetailResponse.TrialInfo.builder()
                .startedAt(trialSession.getStartedAt())
                .expiresAt(trialSession.getExpiresAt())
                .outcome(expired ? "EXPIRED" : trialSession.getOutcome())
                .active(active)
                .expired(expired)
                .acceptedAt(trialSession.getAcceptedAt())
                .rejectedAt(trialSession.getRejectedAt())
                .build();
    }

    private String stringVal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
