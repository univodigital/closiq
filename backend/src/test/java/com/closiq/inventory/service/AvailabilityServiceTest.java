package com.closiq.inventory.service;

import com.closiq.catalog.domain.Product;
import com.closiq.catalog.domain.ProductVariant;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.catalog.repository.ProductVariantRepository;
import com.closiq.config.ClosiqProperties;
import com.closiq.inventory.domain.InventoryItem;
import com.closiq.inventory.domain.InventoryItemStatus;
import com.closiq.inventory.domain.InventoryReservation;
import com.closiq.inventory.repository.InventoryBlockRepository;
import com.closiq.inventory.repository.InventoryItemRepository;
import com.closiq.inventory.repository.InventoryReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("44444444-4444-7444-8444-444444444401");
    private static final UUID VARIANT_ID = UUID.fromString("55555555-5555-7555-8555-555555555502");
    private static final UUID ITEM_ID = UUID.fromString("77777777-7777-7777-8777-777777777702");

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private InventoryReservationRepository reservationRepository;
    @Mock
    private InventoryBlockRepository blockRepository;

    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        ClosiqProperties properties = new ClosiqProperties();
        properties.getInventory().setDefaultAvailabilityDays(7);
        availabilityService = new AvailabilityService(
                productRepository,
                productVariantRepository,
                inventoryItemRepository,
                reservationRepository,
                blockRepository,
                properties);
    }

    @Test
    void getAvailability_marksBookedDatesUnavailable() {
        Product product = Product.builder()
                .id(PRODUCT_ID)
                .slug("emerald-draped-saree")
                .minRentalDays((short) 1)
                .maxRentalDays((short) 14)
                .cleaningBufferDays((short) 1)
                .status("ACTIVE")
                .build();

        ProductVariant variant = ProductVariant.builder().id(VARIANT_ID).product(product).build();
        InventoryItem item = InventoryItem.builder()
                .id(ITEM_ID)
                .productVariant(variant)
                .status(InventoryItemStatus.AVAILABLE)
                .build();

        InventoryReservation reservation = InventoryReservation.builder()
                .inventoryItem(item)
                .startDate(LocalDate.of(2026, 8, 20))
                .endDate(LocalDate.of(2026, 8, 23))
                .reservationType("CONFIRMED")
                .status("ACTIVE")
                .build();

        when(productRepository.findActiveBySlugOrId("emerald-draped-saree", "ACTIVE"))
                .thenReturn(Optional.of(product));
        when(productVariantRepository.findByIdAndProductId(VARIANT_ID, PRODUCT_ID))
                .thenReturn(Optional.of(variant));
        when(inventoryItemRepository.findByProductVariantIdAndStatusNotOrderByCreatedAtAsc(
                eq(VARIANT_ID), eq(InventoryItemStatus.RETIRED)))
                .thenReturn(List.of(item));
        when(reservationRepository.findActiveForVariantInRange(eq(VARIANT_ID), any(), any()))
                .thenReturn(List.of(reservation));
        when(blockRepository.findActiveForVariantInRange(eq(VARIANT_ID), any(), any()))
                .thenReturn(List.of());

        var response = availabilityService.getAvailability(
                "emerald-draped-saree",
                VARIANT_ID,
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 23));

        assertThat(response.getUnavailableDates()).contains(
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 21),
                LocalDate.of(2026, 8, 22),
                LocalDate.of(2026, 8, 23));
        assertThat(response.getBookedRanges()).hasSize(1);
        assertThat(response.getBookedRanges().getFirst().getReason()).isEqualTo("BOOKED");
    }
}
