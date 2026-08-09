package com.closiq.payment.service;

import com.closiq.booking.repository.BookingRepository;
import com.closiq.common.web.PageBoundary;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.payment.domain.Payment;
import com.closiq.payment.domain.Refund;
import com.closiq.payment.repository.PaymentRepository;
import com.closiq.payment.repository.RefundRepository;
import com.closiq.payment.web.dto.PaymentSummaryResponse;
import com.closiq.payment.web.dto.RefundStatusResponse;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public PagedResult<PaymentSummaryResponse> listPayments(
            UUID customerId, String status, UUID bookingId, String pageToken, Integer limit) {

        int pageSize = normalizeLimit(limit);
        PageBoundary boundary = PageTokenCodec.paymentBoundary(pageToken);

        List<Payment> payments = paymentRepository.findCustomerPage(
                customerId,
                status,
                bookingId,
                boundary.beforeCreatedAt(),
                boundary.beforeId(),
                PageRequest.of(0, pageSize + 1));

        boolean hasMore = payments.size() > pageSize;
        List<Payment> pageItems = hasMore ? payments.subList(0, pageSize) : payments;

        List<PaymentSummaryResponse> responses = pageItems.stream()
                .map(payment -> {
                    var booking = bookingRepository.findById(payment.getBookingId()).orElse(null);
                    String rentalNumber = booking != null ? booking.getRentalNumber() : null;
                    String orderNumber = booking != null ? booking.getOrderNumber() : null;
                    return PaymentSummaryResponse.builder()
                            .paymentId(payment.getId().toString())
                            .rentalNumber(rentalNumber)
                            .bookingNumber(rentalNumber)
                            .orderNumber(orderNumber)
                            .amount(payment.getAmount())
                            .amountInRupees(payment.getAmount() / 100)
                            .status(payment.getStatus())
                            .method(payment.getPaymentMethod())
                            .createdAt(payment.getCreatedAt())
                            .build();
                })
                .toList();

        String nextPageToken = hasMore && !pageItems.isEmpty()
                ? PageTokenCodec.encodePayment(new PageTokenCodec.PaymentPageToken(
                        pageItems.getLast().getCreatedAt(),
                        pageItems.getLast().getId()))
                : null;

        return PagedResult.of(responses, pageSize, hasMore, nextPageToken);
    }

    @Transactional(readOnly = true)
    public RefundStatusResponse getRefundStatus(UUID customerId, UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndCustomerId(paymentId, customerId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Payment not found"));

        List<Refund> refunds = refundRepository.findByPaymentIdOrderByInitiatedAtAsc(paymentId);

        return RefundStatusResponse.builder()
                .paymentId(payment.getId().toString())
                .bookingId(payment.getBookingId().toString())
                .refunds(refunds.stream()
                        .map(refund -> RefundStatusResponse.RefundItem.builder()
                                .refundId(refund.getId().toString())
                                .type(refund.getRefundType())
                                .amount(refund.getAmount() / 100)
                                .status(refund.getStatus())
                                .initiatedAt(refund.getInitiatedAt())
                                .processedAt(refund.getProcessedAt())
                                .expectedBy(refund.getExpectedBy())
                                .build())
                        .toList())
                .build();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 20;
        }
        return Math.min(limit, 50);
    }
}
