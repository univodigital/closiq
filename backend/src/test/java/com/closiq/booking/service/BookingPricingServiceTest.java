package com.closiq.booking.service;

import com.closiq.catalog.domain.Product;
import com.closiq.config.ClosiqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BookingPricingServiceTest {

    private BookingPricingService pricingService;

    @BeforeEach
    void setUp() {
        ClosiqProperties properties = new ClosiqProperties();
        pricingService = new BookingPricingService(properties);
    }

    @Test
    void calculate_rentalDepositAndTotal() {
        Product product = Product.builder()
                .pricePerDay(1299)
                .depositAmount(3500)
                .currencyCode("INR")
                .build();

        var pricing = pricingService.calculate(
                product, LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 16));

        assertThat(pricing.getRentalDays()).isEqualTo((short) 3);
        assertThat(pricing.getRentalAmount()).isEqualTo(3897);
        assertThat(pricing.getDepositAmount()).isEqualTo(3500);
        assertThat(pricing.getTotalAmount()).isEqualTo(7397);
    }
}
