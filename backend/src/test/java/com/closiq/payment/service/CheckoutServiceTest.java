package com.closiq.payment.service;

import com.closiq.booking.repository.BookingRepository;
import com.closiq.booking.repository.CheckoutSessionRepository;
import com.closiq.booking.service.BookingHoldExpiryService;
import com.closiq.booking.service.BookingPricingService;
import com.closiq.booking.service.BookingService;
import com.closiq.catalog.domain.Product;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.catalog.repository.ProductVariantRepository;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.inventory.service.AvailabilityService;
import com.closiq.payment.web.dto.CheckoutCalculateBatchRequest;
import com.closiq.payment.web.dto.CheckoutCalculateRequest;
import com.closiq.user.repository.AddressRepository;
import com.closiq.user.repository.ServiceablePincodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private BookingPricingService pricingService;
    @Mock private CouponService couponService;
    @Mock private ServiceablePincodeRepository serviceablePincodeRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private CheckoutSessionRepository checkoutSessionRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private BookingService bookingService;
    @Mock private BookingHoldExpiryService holdExpiryService;
    @Mock private AvailabilityService availabilityService;

    @InjectMocks
    private CheckoutService checkoutService;

    @Test
    void calculate_rejectsUnavailableDates() {
        Product product = Product.builder()
                .id(PRODUCT_ID)
                .cleaningBufferDays((short) 1)
                .status("ACTIVE")
                .build();
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = start.plusDays(2);
        LocalDate effectiveEnd = end.plusDays(1);

        when(productRepository.findByIdAndDeletedAtIsNullAndStatus(PRODUCT_ID, "ACTIVE"))
                .thenReturn(Optional.of(product));
        when(productVariantRepository.findByIdAndProductId(VARIANT_ID, PRODUCT_ID))
                .thenReturn(Optional.of(com.closiq.catalog.domain.ProductVariant.builder().id(VARIANT_ID).build()));
        when(availabilityService.isRangeAvailable(VARIANT_ID, start, effectiveEnd)).thenReturn(false);

        CheckoutCalculateRequest request = new CheckoutCalculateRequest(
                PRODUCT_ID, VARIANT_ID, start, end, null, null);

        assertThatThrownBy(() -> checkoutService.calculate(request))
                .isInstanceOf(ClosiqException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_CONFLICT);

        verify(pricingService, never()).calculate(any(), any(), any());
    }

    @Test
    void calculate_proceedsWhenDatesAvailable() {
        Product product = Product.builder()
                .id(PRODUCT_ID)
                .pricePerDay(1000L)
                .depositAmount(5000L)
                .cleaningBufferDays((short) 1)
                .status("ACTIVE")
                .build();
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = start.plusDays(2);
        LocalDate effectiveEnd = end.plusDays(1);

        when(productRepository.findByIdAndDeletedAtIsNullAndStatus(PRODUCT_ID, "ACTIVE"))
                .thenReturn(Optional.of(product));
        when(productVariantRepository.findByIdAndProductId(VARIANT_ID, PRODUCT_ID))
                .thenReturn(Optional.of(com.closiq.catalog.domain.ProductVariant.builder().id(VARIANT_ID).build()));
        when(availabilityService.isRangeAvailable(VARIANT_ID, start, effectiveEnd)).thenReturn(true);
        when(pricingService.calculate(product, start, end)).thenReturn(
                BookingPricingService.PricingBreakdown.builder()
                        .rentalDays((short) 3)
                        .rentalAmount(3000L)
                        .depositAmount(5000L)
                        .deliveryFee(0L)
                        .discountAmount(0L)
                        .totalAmount(8000L)
                        .currency("INR")
                        .build());

        var response = checkoutService.calculate(new CheckoutCalculateRequest(
                PRODUCT_ID, VARIANT_ID, start, end, null, null));

        org.assertj.core.api.Assertions.assertThat(response.getTotalAmount()).isEqualTo(8000L);
    }

    @Test
    void calculateBatch_appliesCouponOnceOnCombinedSubtotal() {
        Product product = Product.builder()
                .id(PRODUCT_ID)
                .pricePerDay(1000L)
                .depositAmount(5000L)
                .cleaningBufferDays((short) 1)
                .status("ACTIVE")
                .build();
        UUID productId2 = UUID.randomUUID();
        UUID variantId2 = UUID.randomUUID();
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = start.plusDays(2);

        when(productRepository.findByIdAndDeletedAtIsNullAndStatus(any(), eq("ACTIVE")))
                .thenReturn(Optional.of(product));
        when(productVariantRepository.findByIdAndProductId(any(), any()))
                .thenReturn(Optional.of(com.closiq.catalog.domain.ProductVariant.builder().id(VARIANT_ID).build()));
        when(availabilityService.isRangeAvailable(any(), any(), any())).thenReturn(true);
        when(pricingService.calculate(any(), any(), any())).thenReturn(
                BookingPricingService.PricingBreakdown.builder()
                        .rentalDays((short) 3)
                        .rentalAmount(3000L)
                        .depositAmount(5000L)
                        .deliveryFee(0L)
                        .discountAmount(0L)
                        .totalAmount(8000L)
                        .currency("INR")
                        .build());
        when(couponService.validate("SAVE500", 16000L))
                .thenReturn(new CouponService.CouponValidation("SAVE500", 500L, "Applied"));

        var response = checkoutService.calculateBatch(new CheckoutCalculateBatchRequest(
                List.of(
                        new CheckoutCalculateBatchRequest.LineItem(
                                PRODUCT_ID, VARIANT_ID, start, end),
                        new CheckoutCalculateBatchRequest.LineItem(
                                productId2, variantId2, start, end)),
                null,
                "SAVE500"));

        org.assertj.core.api.Assertions.assertThat(response.getSubtotal()).isEqualTo(16000L);
        org.assertj.core.api.Assertions.assertThat(response.getDiscountAmount()).isEqualTo(500L);
        org.assertj.core.api.Assertions.assertThat(response.getTotalAmount()).isEqualTo(15500L);
    }
}
