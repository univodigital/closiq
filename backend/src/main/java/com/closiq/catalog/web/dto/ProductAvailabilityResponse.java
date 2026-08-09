package com.closiq.catalog.web.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class ProductAvailabilityResponse {

    String productId;
    String variantId;
    short minRentalDays;
    Short maxRentalDays;
    short bufferDaysAfterReturn;
    List<LocalDate> unavailableDates;
    List<DateRange> bookedRanges;
    List<DateRange> blockedRanges;
    LocalDate nextAvailableDate;

    @Value
    @Builder
    public static class DateRange {
        LocalDate start;
        LocalDate end;
        String reason;
    }
}
