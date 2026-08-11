package com.closiq.booking.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class BookingDetailResponse {

    String id;
    String rentalNumber;
    /** @deprecated Use {@link #rentalNumber}. Kept for backward compatibility. */
    String bookingNumber;
    String orderNumber;
    String status;
    String productId;
    String productTitle;
    String productImage;
    String variantSize;
    LocalDate rentalStartDate;
    LocalDate rentalEndDate;
    short rentalDays;
    long rentalAmount;
    long depositAmount;
    long deliveryFee;
    long discountAmount;
    long totalAmount;
    String currency;
    boolean includesTrial;
    short trialDurationMinutes;
    TrialInfo trialInfo;
    Instant holdExpiresAt;
    Instant createdAt;
    DeliveryAddress deliveryAddress;
    PaymentSummary paymentSummary;
    RefundDetails refundDetails;
    DepositSummary depositSummary;
    ReturnPickupSummary returnPickup;
    CancellationInfo cancellation;
    boolean invoiceAvailable;
    int depositRefundExpectedBusinessDays;
    List<TimelineEventResponse> timeline;

    @Value
    @Builder
    public static class PaymentSummary {
        String paymentId;
        String status;
        String method;
        long rentalAmount;
        long depositAmount;
        long deliveryFee;
        long discountAmount;
        long totalPaid;
        Instant paidAt;
        String checkoutBatchId;
        boolean paymentPending;
    }

    @Value
    @Builder
    public static class RefundDetails {
        long refundAmount;
        long depositRefundAmount;
        long totalRefunded;
        String status;
        String refundMethod;
        Integer expectedBusinessDays;
        Instant expectedBy;
        List<RefundItem> items;
    }

    @Value
    @Builder
    public static class ReturnPickupSummary {
        UUID shipmentId;
        String returnReference;
        String status;
        java.time.LocalDate pickupDate;
        String pickupWindow;
        Instant pickupScheduledAt;
        Instant pickedUpAt;
        Instant completedAt;
        String agentName;
    }

    @Value
    @Builder
    public static class DepositSummary {
        String depositStatus;
        String inspectionStatus;
        long depositAmount;
        long damageDeduction;
        long lateFee;
        long cleaningFee;
        long totalDeduction;
        String deductionReason;
        long refundAmount;
        String refundStatus;
        String refundMethod;
        String expectedRefundWindow;
    }

    @Value
    @Builder
    public static class CancellationInfo {
        boolean eligible;
        String policyLabel;
    }

    @Value
    @Builder
    public static class RefundItem {
        String refundId;
        String type;
        long amount;
        String status;
        Instant initiatedAt;
        Instant processedAt;
        Instant expectedBy;
    }

    @Value
    @Builder
    public static class TrialInfo {
        Instant startedAt;
        Instant expiresAt;
        String outcome;
        boolean active;
        boolean expired;
        Instant acceptedAt;
        Instant rejectedAt;
    }

    @Value
    @Builder
    public static class DeliveryAddress {
        String line1;
        String line2;
        String city;
        String state;
        String pincode;
    }
}
