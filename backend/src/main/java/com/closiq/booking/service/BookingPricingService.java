package com.closiq.booking.service;

import com.closiq.catalog.domain.Product;
import com.closiq.config.ClosiqProperties;
import lombok.Builder;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class BookingPricingService {

    private final ClosiqProperties properties;

    public BookingPricingService(ClosiqProperties properties) {
        this.properties = properties;
    }

    public PricingBreakdown calculate(Product product, LocalDate rentalStartDate, LocalDate rentalEndDate) {
        long rentalDays = ChronoUnit.DAYS.between(rentalStartDate, rentalEndDate) + 1;
        long rentalAmount = product.getPricePerDay() * rentalDays;
        long depositAmount = product.getDepositAmount();
        long deliveryFee = properties.getBooking().getDeliveryFeeDefault();
        long discountAmount = 0;
        long totalAmount = rentalAmount + depositAmount + deliveryFee - discountAmount;

        return PricingBreakdown.builder()
                .rentalDays((short) rentalDays)
                .rentalAmount(rentalAmount)
                .depositAmount(depositAmount)
                .deliveryFee(deliveryFee)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .currency(product.getCurrencyCode())
                .build();
    }

    @Value
    @Builder
    public static class PricingBreakdown {
        short rentalDays;
        long rentalAmount;
        long depositAmount;
        long deliveryFee;
        long discountAmount;
        long totalAmount;
        String currency;
    }
}
