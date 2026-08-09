package com.closiq.inventory.service;

import com.closiq.catalog.domain.Product;
import com.closiq.catalog.domain.ProductVariant;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.catalog.repository.ProductVariantRepository;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.IdGenerator;
import com.closiq.inventory.domain.InventoryBlock;
import com.closiq.inventory.domain.InventoryItem;
import com.closiq.inventory.domain.InventoryItemStatus;
import com.closiq.inventory.repository.InventoryBlockRepository;
import com.closiq.inventory.service.InventoryHistoryService;
import com.closiq.inventory.service.InventoryStockService;
import com.closiq.inventory.repository.InventoryItemRepository;
import com.closiq.inventory.repository.InventoryReservationRepository;
import com.closiq.inventory.web.dto.CreateInventoryBlockRequest;
import com.closiq.inventory.web.dto.InventoryBlockResponse;
import com.closiq.inventory.web.dto.SellerInventoryResponse;
import com.closiq.inventory.web.dto.UpdateSellerInventoryRequest;
import com.closiq.seller.service.SellerProductAccessService;
import com.closiq.seller.web.dto.SellerInventoryBlockListItemResponse;
import com.closiq.user.domain.SellerProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerInventoryService {

    private static final String ACTIVE_VARIANT = "ACTIVE";
    private static final String ACTIVE_BLOCK = "ACTIVE";
    private static final String REMOVED_BLOCK = "REMOVED";

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryBlockRepository blockRepository;
    private final InventoryHistoryService historyService;
    private final InventoryStockService stockService;
    private final SellerProductAccessService productAccessService;

    @Transactional(readOnly = true)
    public List<SellerInventoryBlockListItemResponse> listBlocks(SellerProfile seller) {
        return blockRepository.findActiveBySellerProfileId(seller.getId()).stream()
                .map(block -> {
                    var variant = block.getProductVariant();
                    var product = variant.getProduct();
                    return SellerInventoryBlockListItemResponse.builder()
                            .id(block.getId().toString())
                            .productId(product.getId().toString())
                            .productTitle(product.getTitle())
                            .variantId(variant.getId().toString())
                            .variantSize(variant.getVariantLabel())
                            .startDate(block.getStartDate())
                            .endDate(block.getEndDate())
                            .reason(block.getReason())
                            .status("BLOCKED")
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public SellerInventoryResponse getInventory(SellerProfile seller, UUID productId) {
        Product product = requireOwnedProduct(seller, productId);
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderBySortOrderAsc(product.getId());

        List<SellerInventoryResponse.VariantInventory> variantRows = variants.stream()
                .map(variant -> SellerInventoryResponse.VariantInventory.builder()
                        .variantId(variant.getId().toString())
                        .size(variant.getVariantLabel())
                        .quantity(stockService.countAvailableUnits(variant.getId()))
                        .available(ACTIVE_VARIANT.equals(variant.getStatus())
                                && stockService.countAvailableUnits(variant.getId()) > 0)
                        .bookedDates((int) reservationRepository.countActiveBookingsForVariant(variant.getId()))
                        .build())
                .toList();

        return SellerInventoryResponse.builder()
                .productId(product.getId().toString())
                .variants(variantRows)
                .build();
    }

    @Transactional
    public SellerInventoryResponse updateInventory(
            SellerProfile seller, UUID productId, UpdateSellerInventoryRequest request, UUID actorId) {

        Product product = requireOwnedProduct(seller, productId);

        for (UpdateSellerInventoryRequest.VariantQuantity update : request.getVariants()) {
            UUID variantId = UUID.fromString(update.getVariantId());
            ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, product.getId())
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Variant not found"));

            int current = stockService.countAvailableUnits(variantId);
            int target = update.getQuantity();
            if (target < 0) {
                throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Quantity cannot be negative");
            }

            if (target > current) {
                addUnits(variant, target - current, actorId);
            } else if (target < current) {
                retireUnits(variant, current - target, actorId);
            }
        }

        return getInventory(seller, productId);
    }

    @Transactional
    public InventoryBlockResponse createBlock(SellerProfile seller, CreateInventoryBlockRequest request, UUID actorId) {
        Product product = requireOwnedProduct(seller, UUID.fromString(request.getProductId()));
        UUID variantId = UUID.fromString(request.getVariantId());
        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, product.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Variant not found"));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "endDate must be on or after startDate");
        }

        InventoryBlock block = InventoryBlock.builder()
                .id(IdGenerator.uuidV7())
                .productVariant(variant)
                .inventoryItem(null)
                .createdBy(actorId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .status(ACTIVE_BLOCK)
                .build();
        blockRepository.save(block);

        return InventoryBlockResponse.builder()
                .id(block.getId().toString())
                .productId(product.getId().toString())
                .variantId(variantId.toString())
                .startDate(block.getStartDate())
                .endDate(block.getEndDate())
                .reason(block.getReason())
                .status("BLOCKED")
                .build();
    }

    @Transactional
    public void removeBlock(SellerProfile seller, UUID userId, UUID blockId) {
        InventoryBlock block = blockRepository.findByIdAndCreatedByAndStatus(blockId, userId, ACTIVE_BLOCK)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Block not found"));

        requireOwnedProduct(seller, block.getProductVariant().getProduct().getId());
        block.setStatus(REMOVED_BLOCK);
        blockRepository.save(block);
    }

    private void addUnits(ProductVariant variant, int count, UUID actorId) {
        for (int i = 0; i < count; i++) {
            String serial = variant.getSku() + "-" + IdGenerator.uuidV7().toString().substring(0, 8).toUpperCase();
            InventoryItem item = InventoryItem.builder()
                    .id(IdGenerator.uuidV7())
                    .productVariant(variant)
                    .serialNumber(serial)
                    .conditionGrade("EXCELLENT")
                    .status(InventoryItemStatus.AVAILABLE)
                    .acquiredAt(Instant.now())
                    .build();
            inventoryItemRepository.save(item);
            historyService.recordItemCreated(item, actorId);
        }
    }

    private void retireUnits(ProductVariant variant, int count, UUID actorId) {
        List<InventoryItem> available = inventoryItemRepository.findByProductVariantIdAndStatusOrderByCreatedAtAsc(
                variant.getId(), InventoryItemStatus.AVAILABLE);
        if (available.size() < count) {
            throw new ClosiqException(
                    ErrorCode.VALIDATION_ERROR,
                    "Cannot reduce quantity below committed units; retire only available stock");
        }

        for (int i = 0; i < count; i++) {
            InventoryItem item = available.get(i);
            String fromStatus = item.getStatus();
            item.setStatus(InventoryItemStatus.RETIRED);
            item.setRetiredAt(Instant.now());
            item.setRetireReason("Seller quantity adjustment");
            inventoryItemRepository.save(item);
            historyService.recordStatusChange(item, fromStatus, InventoryItemStatus.RETIRED, actorId, "QUANTITY_REDUCED");
        }
    }

    private Product requireOwnedProduct(SellerProfile seller, UUID productId) {
        return productAccessService.requireOwnedProduct(seller, productId);
    }
}
