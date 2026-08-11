package com.closiq.inventory.service;

import com.closiq.catalog.domain.Product;
import com.closiq.catalog.domain.ProductVariant;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.catalog.repository.ProductVariantRepository;
import com.closiq.catalog.web.dto.ProductAvailabilityResponse;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.config.ClosiqProperties;
import com.closiq.inventory.domain.InventoryBlock;
import com.closiq.inventory.domain.InventoryItem;
import com.closiq.inventory.domain.InventoryItemStatus;
import com.closiq.inventory.domain.InventoryReservation;
import com.closiq.inventory.repository.InventoryBlockRepository;
import com.closiq.inventory.repository.InventoryItemRepository;
import com.closiq.inventory.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private static final String ACTIVE_PRODUCT = "ACTIVE";
    private static final String ACTIVE_VARIANT = "ACTIVE";
    private static final String ACTIVE_RESERVATION = "ACTIVE";
    private static final String ACTIVE_BLOCK = "ACTIVE";

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryBlockRepository blockRepository;
    private final ClosiqProperties properties;

    @Transactional(readOnly = true)
    public ProductAvailabilityResponse getAvailability(
            String slugOrId, UUID variantId, LocalDate startDate, LocalDate endDate) {

        Product product = productRepository.findActiveBySlugOrId(slugOrId, ACTIVE_PRODUCT)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Product not found"));

        productVariantRepository.findByIdAndProductId(variantId, product.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Variant not found"));

        LocalDate rangeStart = startDate != null ? startDate : LocalDate.now();
        LocalDate rangeEnd = endDate != null
                ? endDate
                : rangeStart.plusDays(properties.getInventory().getDefaultAvailabilityDays());

        if (rangeEnd.isBefore(rangeStart)) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "endDate must be on or after startDate");
        }

        List<InventoryItem> items = inventoryItemRepository.findByProductVariantIdAndStatusNotOrderByCreatedAtAsc(
                variantId, InventoryItemStatus.RETIRED);
        List<InventoryReservation> reservations = reservationRepository.findActiveForVariantInRange(
                variantId, rangeStart, rangeEnd);
        List<InventoryBlock> blocks = blockRepository.findActiveForVariantInRange(
                variantId, rangeStart, rangeEnd);

        List<LocalDate> unavailableDates = new ArrayList<>();
        LocalDate nextAvailable = null;

        for (LocalDate date = rangeStart; !date.isAfter(rangeEnd); date = date.plusDays(1)) {
            if (countAvailableOnDate(date, items, reservations, blocks) == 0) {
                unavailableDates.add(date);
            } else if (nextAvailable == null) {
                nextAvailable = date;
            }
        }

        if (nextAvailable == null) {
            nextAvailable = rangeEnd.plusDays(1);
        }

        return ProductAvailabilityResponse.builder()
                .productId(product.getId().toString())
                .variantId(variantId.toString())
                .minRentalDays(product.getMinRentalDays())
                .maxRentalDays(product.getMaxRentalDays())
                .bufferDaysAfterReturn(product.getCleaningBufferDays())
                .unavailableDates(unavailableDates)
                .bookedRanges(toBookedRanges(reservations))
                .blockedRanges(toBlockedRanges(blocks))
                .nextAvailableDate(nextAvailable)
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isVariantAvailableOnDate(UUID variantId, LocalDate date) {
        List<InventoryItem> items = inventoryItemRepository.findByProductVariantIdAndStatusNotOrderByCreatedAtAsc(
                variantId, InventoryItemStatus.RETIRED);
        if (items.isEmpty()) {
            return false;
        }
        List<InventoryReservation> reservations = reservationRepository.findActiveForVariantInRange(
                variantId, date, date);
        List<InventoryBlock> blocks = blockRepository.findActiveForVariantInRange(
                variantId, date, date);
        return countAvailableOnDate(date, items, reservations, blocks) > 0;
    }

    @Transactional(readOnly = true)
    public boolean isRangeAvailable(UUID variantId, LocalDate startDate, LocalDate effectiveEndDate) {
        return selectAvailableItem(variantId, startDate, effectiveEndDate).isPresent();
    }

    /**
     * Batch availability for product listing — one query set for all variants on the page.
     * A product is available when any ACTIVE variant has a free inventory item for the full range
     * (rental end + cleaning buffer).
     */
    @Transactional(readOnly = true)
    public Map<UUID, Boolean> areProductsAvailableForDates(
            List<Product> products, LocalDate startDate, LocalDate endDate) {

        if (products.isEmpty() || startDate == null || endDate == null) {
            return Map.of();
        }
        if (endDate.isBefore(startDate)) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "endDate must be on or after startDate");
        }

        List<UUID> productIds = products.stream().map(Product::getId).toList();
        List<ProductVariant> variants = productVariantRepository.findByProductIdInAndStatusOrderBySortOrderAsc(
                productIds, ACTIVE_VARIANT);

        Map<UUID, List<ProductVariant>> variantsByProduct = variants.stream()
                .collect(Collectors.groupingBy(v -> v.getProduct().getId()));

        List<UUID> variantIds = variants.stream().map(ProductVariant::getId).toList();
        if (variantIds.isEmpty()) {
            return products.stream().collect(Collectors.toMap(Product::getId, p -> false));
        }

        LocalDate rangeEnd = endDate;
        for (Product product : products) {
            LocalDate effective = endDate.plusDays(product.getCleaningBufferDays());
            if (effective.isAfter(rangeEnd)) {
                rangeEnd = effective;
            }
        }

        List<InventoryItem> items = inventoryItemRepository.findRentableByVariantIds(
                variantIds, InventoryItemStatus.RETIRED);
        List<InventoryReservation> reservations = reservationRepository.findActiveForVariantsInRange(
                variantIds, startDate, rangeEnd);
        List<InventoryBlock> blocks = blockRepository.findActiveForVariantsInRange(
                variantIds, startDate, rangeEnd);

        Map<UUID, List<InventoryItem>> itemsByVariant = items.stream()
                .collect(Collectors.groupingBy(i -> i.getProductVariant().getId()));

        Map<UUID, Boolean> result = new HashMap<>();
        for (Product product : products) {
            LocalDate effectiveEnd = endDate.plusDays(product.getCleaningBufferDays());
            boolean available = false;
            for (ProductVariant variant : variantsByProduct.getOrDefault(product.getId(), List.of())) {
                for (InventoryItem item : itemsByVariant.getOrDefault(variant.getId(), List.of())) {
                    if (InventoryItemStatus.MAINTENANCE.equals(item.getStatus())) {
                        continue;
                    }
                    boolean freeForRange = true;
                    for (LocalDate date = startDate; !date.isAfter(effectiveEnd); date = date.plusDays(1)) {
                        if (!isItemFreeOnDate(item.getId(), date, reservations, blocks)) {
                            freeForRange = false;
                            break;
                        }
                    }
                    if (freeForRange) {
                        available = true;
                        break;
                    }
                }
                if (available) {
                    break;
                }
            }
            result.put(product.getId(), available);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<InventoryItem> selectAvailableItem(
            UUID variantId, LocalDate startDate, LocalDate effectiveEndDate) {

        List<InventoryItem> items = inventoryItemRepository.findByProductVariantIdAndStatusNotOrderByCreatedAtAsc(
                variantId, InventoryItemStatus.RETIRED);
        if (items.isEmpty()) {
            return java.util.Optional.empty();
        }

        List<InventoryReservation> reservations = reservationRepository.findActiveForVariantInRange(
                variantId, startDate, effectiveEndDate);
        List<InventoryBlock> blocks = blockRepository.findActiveForVariantInRange(
                variantId, startDate, effectiveEndDate);

        for (InventoryItem item : items) {
            if (InventoryItemStatus.MAINTENANCE.equals(item.getStatus())) {
                continue;
            }
            boolean freeForRange = true;
            for (LocalDate date = startDate; !date.isAfter(effectiveEndDate); date = date.plusDays(1)) {
                if (!isItemFreeOnDate(item.getId(), date, reservations, blocks)) {
                    freeForRange = false;
                    break;
                }
            }
            if (freeForRange) {
                return java.util.Optional.of(item);
            }
        }
        return java.util.Optional.empty();
    }

    private int countAvailableOnDate(
            LocalDate date,
            List<InventoryItem> items,
            List<InventoryReservation> reservations,
            List<InventoryBlock> blocks) {

        int available = 0;
        for (InventoryItem item : items) {
            if (InventoryItemStatus.MAINTENANCE.equals(item.getStatus())) {
                continue;
            }
            if (!isItemFreeOnDate(item.getId(), date, reservations, blocks)) {
                continue;
            }
            available++;
        }
        return available;
    }

    private boolean isItemFreeOnDate(
            UUID itemId,
            LocalDate date,
            List<InventoryReservation> reservations,
            List<InventoryBlock> blocks) {

        for (InventoryReservation reservation : reservations) {
            if (!reservation.getInventoryItem().getId().equals(itemId)) {
                continue;
            }
            if (!date.isBefore(reservation.getStartDate()) && !date.isAfter(reservation.getEndDate())) {
                return false;
            }
        }

        for (InventoryBlock block : blocks) {
            if (block.getInventoryItem() != null && !block.getInventoryItem().getId().equals(itemId)) {
                continue;
            }
            if (!date.isBefore(block.getStartDate()) && !date.isAfter(block.getEndDate())) {
                return false;
            }
        }

        return true;
    }

    private List<ProductAvailabilityResponse.DateRange> toBookedRanges(List<InventoryReservation> reservations) {
        Set<String> seen = new HashSet<>();
        List<ProductAvailabilityResponse.DateRange> ranges = new ArrayList<>();
        for (InventoryReservation reservation : reservations) {
            if ("BUFFER".equals(reservation.getReservationType())) {
                continue;
            }
            String key = reservation.getStartDate() + ":" + reservation.getEndDate();
            if (!seen.add(key)) {
                continue;
            }
            ranges.add(ProductAvailabilityResponse.DateRange.builder()
                    .start(reservation.getStartDate())
                    .end(reservation.getEndDate())
                    .reason("BOOKED")
                    .build());
        }
        return ranges;
    }

    private List<ProductAvailabilityResponse.DateRange> toBlockedRanges(List<InventoryBlock> blocks) {
        return blocks.stream()
                .map(block -> ProductAvailabilityResponse.DateRange.builder()
                        .start(block.getStartDate())
                        .end(block.getEndDate())
                        .reason("SELLER_BLOCKED")
                        .build())
                .toList();
    }
}
