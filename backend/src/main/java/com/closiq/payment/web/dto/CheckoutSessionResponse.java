package com.closiq.payment.web.dto;

import com.closiq.booking.web.dto.BookingDetailResponse;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckoutSessionResponse {

    String sessionId;
    String bookingId;
    boolean readyForPayment;
    long totalAmount;
    long discountAmount;
    String currency;
    BookingDetailResponse booking;
}
