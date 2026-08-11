package com.closiq.seller.service;

import com.closiq.booking.repository.BookingItemRepository;
import com.closiq.catalog.domain.Brand;
import com.closiq.catalog.domain.Category;
import com.closiq.catalog.domain.Product;
import com.closiq.catalog.domain.ProductImage;
import com.closiq.catalog.domain.ProductStatus;
import com.closiq.catalog.domain.ProductVariant;
import com.closiq.catalog.mapper.ProductMapper;
import com.closiq.catalog.repository.BrandRepository;
import com.closiq.catalog.repository.CategoryRepository;
import com.closiq.catalog.repository.ProductImageRepository;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.catalog.repository.ProductVariantRepository;
import com.closiq.catalog.web.dto.ProductDetailResponse;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.identifier.ProductCodeGenerator;
import com.closiq.common.util.IdGenerator;
import com.closiq.common.util.SlugGenerator;
import com.closiq.common.util.SlugUtils;
import com.closiq.common.web.PageBoundary;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.storage.FileStorageService;
import com.closiq.storage.MediaAssetFactory;
import com.closiq.storage.MediaUploadMapper;
import com.closiq.storage.StoredUploadResult;
import com.closiq.identity.domain.User;
import com.closiq.identity.service.UserService;
import com.closiq.inventory.domain.InventoryItem;
import com.closiq.inventory.domain.InventoryItemStatus;
import com.closiq.inventory.repository.InventoryItemRepository;
import com.closiq.inventory.service.InventoryHistoryService;
import com.closiq.inventory.service.InventoryStockService;
import com.closiq.seller.domain.MediaAsset;
import com.closiq.seller.repository.MediaAssetRepository;
import com.closiq.seller.web.dto.ConfirmProductImageRequest;
import com.closiq.seller.web.dto.CreateSellerProductRequest;
import com.closiq.seller.web.dto.DuplicateProductResponse;
import com.closiq.seller.web.dto.PresignedUploadResponse;
import com.closiq.seller.web.dto.ProductImageAttachResponse;
import com.closiq.seller.web.dto.ProductImageUploadUrlRequest;
import com.closiq.seller.web.dto.PublishProductResponse;
import com.closiq.seller.web.dto.SellerProductDetailResponse;
import com.closiq.seller.web.dto.SellerProductListItemResponse;
import com.closiq.seller.web.dto.SellerProductResponse;
import com.closiq.seller.web.dto.UpdateSellerProductRequest;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerProductService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private static final String ACTIVE_CATEGORY = "ACTIVE";
    private static final String ACTIVE_BRAND = "ACTIVE";
    private static final String ACTIVE_VARIANT = "ACTIVE";

    private final SellerContextService sellerContextService;
    private final SellerProductAccessService productAccessService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final BookingItemRepository bookingItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryHistoryService inventoryHistoryService;
    private final InventoryStockService inventoryStockService;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaAssetCleanupService mediaAssetCleanupService;
    private final FileStorageService fileStorageService;
    private final MediaAssetFactory mediaAssetFactory;
    private final MediaUploadMapper mediaUploadMapper;
    private final UserService userService;
    private final ProductCodeGenerator productCodeGenerator;
    private final SlugGenerator slugGenerator;
    private final ProductMapper productMapper;
    private final SellerProfileRepository sellerProfileRepository;

    @Transactional
    public SellerProductResponse createProduct(
            UUID userId, String idempotencyKey, CreateSellerProductRequest request) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key header is required");
        }

        var existing = productRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Product product = existing.get();
            if (!matchesCreateRequest(product, request)) {
                throw new ClosiqException(ErrorCode.IDEMPOTENCY_CONFLICT);
            }
            return toProductResponse(product);
        }

        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        validatePricing(request.getPricePerDay(), request.getDeposit());

        Category category = resolveCategory(request.getCategoryId(), request.getOccasion());
        Brand brand = resolveBrand(request.getDesigner());

        String slug = slugGenerator.uniqueSlug(request.getTitle(), productRepository::existsBySlugAndDeletedAtIsNull);
        UUID productId = IdGenerator.uuidV7();

        Product product = Product.builder()
                .id(productId)
                .productCode(productCodeGenerator.nextCode())
                .slug(slug)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .brand(brand)
                .pricePerDay(request.getPricePerDay())
                .depositAmount(request.getDeposit())
                .currencyCode("INR")
                .sellerProfileId(seller.getId())
                .city(request.getCity())
                .audience(request.getAudience().toLowerCase(Locale.ROOT))
                .garmentType(request.getGarmentType())
                .minRentalDays((short) 1)
                .maxRentalDays((short) 14)
                .cleaningBufferDays((short) 1)
                .includesTrial(true)
                .trialDurationMinutes((short) 15)
                .featured(false)
                .trending(false)
                .status(ProductStatus.DRAFT)
                .reviewCount(0)
                .idempotencyKey(idempotencyKey)
                .build();
        product.setCreatedBy(userId);
        product.setUpdatedBy(userId);
        productRepository.save(product);

        short sort = 0;
        for (CreateSellerProductRequest.VariantInput variantInput : request.getVariants()) {
            ProductVariant variant = ProductVariant.builder()
                    .id(IdGenerator.uuidV7())
                    .product(product)
                    .sku(buildSku(slug, variantInput.getSize()))
                    .variantLabel(variantInput.getSize().toUpperCase(Locale.ROOT))
                    .status(ACTIVE_VARIANT)
                    .sortOrder(++sort)
                    .createdAt(Instant.now())
                    .build();
            productVariantRepository.save(variant);
            createInventoryUnits(variant, variantInput.getQuantity(), userId);
        }

        return toProductResponse(product);
    }

    @Transactional
    public SellerProductResponse updateProduct(
            UUID userId, UUID productId, UpdateSellerProductRequest request) {

        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Product product = productAccessService.requireOwnedProduct(seller, productId);

        if (request.getPricePerDay() != null || request.getDeposit() != null) {
            long price = request.getPricePerDay() != null ? request.getPricePerDay() : product.getPricePerDay();
            long deposit = request.getDeposit() != null ? request.getDeposit() : product.getDepositAmount();
            validatePricing(price, deposit);
            if (hasActiveBookings(product.getId())
                    && ((request.getPricePerDay() != null && request.getPricePerDay() != product.getPricePerDay())
                            || (request.getDeposit() != null && request.getDeposit() != product.getDepositAmount()))) {
                throw new ClosiqException(
                        ErrorCode.INVALID_STATE_TRANSITION, "Cannot change pricing while active bookings exist");
            }
            product.setPricePerDay(price);
            product.setDepositAmount(deposit);
        }

        if (request.getTitle() != null) {
            product.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getCity() != null) {
            product.setCity(request.getCity());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findByIdAndStatus(request.getCategoryId(), ACTIVE_CATEGORY)
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Category not found"));
            product.setCategory(category);
        }
        if (request.getDesigner() != null) {
            product.setBrand(resolveBrand(request.getDesigner()));
        }

        product.setUpdatedBy(userId);
        productRepository.save(product);
        return toProductResponse(product);
    }

    @Transactional
    public void deleteProduct(UUID userId, UUID productId) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Product product = productAccessService.requireOwnedProduct(seller, productId);

        if (hasActiveBookings(product.getId())) {
            throw new ClosiqException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    "Cannot delete product with future bookings; unpublish instead");
        }

        product.setStatus(ProductStatus.ARCHIVED);
        product.setUpdatedBy(userId);
        productRepository.save(product);
    }

    @Transactional
    public PublishProductResponse publishProduct(UUID userId, UUID productId) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Product product = productAccessService.requireOwnedProduct(seller, productId);

        if (!ProductStatus.DRAFT.equals(product.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Only draft products can be published");
        }

        if (product.getTitle() == null || product.getDescription() == null || product.getCategory() == null) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Missing required product fields");
        }

        if (productImageRepository.countByProductId(product.getId()) < 1) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "At least one product image is required");
        }

        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        if (variants.isEmpty()) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "At least one variant is required");
        }

        Instant now = Instant.now();
        product.setStatus(ProductStatus.ACTIVE);
        product.setPublishedAt(now);
        product.setUpdatedBy(userId);

        if (product.getPrimaryImageUrl() == null) {
            productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId()).stream()
                    .findFirst()
                    .ifPresent(image -> product.setPrimaryImageUrl(image.getImageUrl()));
        }

        productRepository.save(product);

        return PublishProductResponse.builder()
                .productId(product.getId())
                .status(ProductStatus.ACTIVE)
                .publishedAt(now)
                .build();
    }

    @Transactional
    public PublishProductResponse unpublishProduct(UUID userId, UUID productId) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Product product = productAccessService.requireOwnedProduct(seller, productId);

        if (!ProductStatus.ACTIVE.equals(product.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Only active listings can be unpublished");
        }

        if (hasActiveBookings(product.getId())) {
            throw new ClosiqException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    "Cannot unpublish while future bookings exist");
        }

        product.setStatus(ProductStatus.DRAFT);
        product.setUpdatedBy(userId);
        productRepository.save(product);

        return PublishProductResponse.builder()
                .productId(product.getId())
                .status(ProductStatus.DRAFT)
                .publishedAt(product.getPublishedAt())
                .build();
    }

    @Transactional
    public PublishProductResponse restoreProduct(UUID userId, UUID productId) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Product product = productAccessService.requireOwnedProduct(seller, productId);

        if (!ProductStatus.ARCHIVED.equals(product.getStatus())) {
            throw new ClosiqException(
                    ErrorCode.INVALID_STATE_TRANSITION, "Only archived listings can be restored");
        }

        product.setStatus(ProductStatus.DRAFT);
        product.setDeletedAt(null);
        product.setPublishedAt(null);
        product.setUpdatedBy(userId);
        productRepository.save(product);

        return PublishProductResponse.builder()
                .productId(product.getId())
                .status(ProductStatus.DRAFT)
                .publishedAt(null)
                .build();
    }

    @Transactional
    public PresignedUploadResponse createImageUploadUrl(
            UUID userId, UUID productId, ProductImageUploadUrlRequest request) {

        validateImageContentType(request.getContentType());

        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Product product = productAccessService.requireOwnedProduct(seller, productId);
        User user = userService.requireActiveUser(userId);

        UUID uploadId = IdGenerator.uuidV7();
        String relativePath = "products/" + product.getId() + "/" + uploadId;

        MediaAsset asset = mediaAssetFactory.createPendingUpload(
                uploadId, user, relativePath, request.getFileName(), request.getContentType());
        mediaAssetRepository.save(asset);

        var instruction = fileStorageService.createUploadInstruction(relativePath, request.getContentType());
        return mediaUploadMapper.toPresignedResponse(uploadId, instruction);
    }

    @Transactional
    public ProductImageAttachResponse uploadProductImage(
            UUID userId,
            UUID productId,
            byte[] fileBytes,
            String fileName,
            String contentType,
            short sortOrder,
            String alt) {

        validateImageContentType(contentType);
        if (fileBytes == null || fileBytes.length == 0) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Image file is required");
        }
        if (fileBytes.length > 10 * 1024 * 1024) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Each image must be 10 MB or smaller");
        }

        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Product product = productAccessService.requireOwnedProduct(seller, productId);
        User user = userService.requireActiveUser(userId);

        UUID uploadId = IdGenerator.uuidV7();
        String relativePath = "products/" + product.getId() + "/" + uploadId;

        MediaAsset asset = mediaAssetFactory.createPendingUpload(
                uploadId, user, relativePath, fileName, contentType);
        mediaAssetRepository.save(asset);

        try {
            StoredUploadResult uploaded = fileStorageService.uploadBytes(
                    relativePath, contentType, fileName, fileBytes);

            asset.setStatus("ATTACHED");
            mediaAssetRepository.save(asset);

            ProductImage image = ProductImage.builder()
                    .id(IdGenerator.uuidV7())
                    .product(product)
                    .imageUrl(uploaded.getPublicUrl())
                    .altText(alt)
                    .sortOrder(sortOrder)
                    .createdAt(Instant.now())
                    .build();
            productImageRepository.save(image);

            if (product.getPrimaryImageUrl() == null || sortOrder == 0) {
                product.setPrimaryImageUrl(uploaded.getPublicUrl());
                productRepository.save(product);
            }

            return ProductImageAttachResponse.builder()
                    .imageId(image.getId().toString())
                    .url(image.getImageUrl())
                    .sortOrder(image.getSortOrder())
                    .alt(image.getAltText())
                    .build();
        } catch (RuntimeException ex) {
            log.warn("Product image upload failed for product {}: {}", productId, ex.getMessage());
            mediaAssetCleanupService.orphanAndDelete(asset);
            throw ex;
        }
    }

    @Transactional
    public ProductImageAttachResponse confirmImage(
            UUID userId, UUID productId, ConfirmProductImageRequest request) {

        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Product product = productAccessService.requireOwnedProduct(seller, productId);

        UUID uploadId = UUID.fromString(request.getUploadId());
        MediaAsset asset = mediaAssetRepository.findByIdAndUploadedById(uploadId, userId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Upload not found"));

        if ("ORPHANED".equals(asset.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Upload was cancelled");
        }

        String publicUrl = fileStorageService.resolvePublicUrl(asset);
        if ("ATTACHED".equals(asset.getStatus())) {
            return productImageRepository.findByProductIdAndImageUrl(product.getId(), publicUrl)
                    .map(image -> ProductImageAttachResponse.builder()
                            .imageId(image.getId().toString())
                            .url(image.getImageUrl())
                            .sortOrder(image.getSortOrder())
                            .alt(image.getAltText())
                            .build())
                    .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Attached image not found"));
        }

        try {
            asset.setStatus("ATTACHED");
            mediaAssetRepository.save(asset);

            ProductImage image = ProductImage.builder()
                    .id(IdGenerator.uuidV7())
                    .product(product)
                    .imageUrl(publicUrl)
                    .altText(request.getAlt())
                    .sortOrder(request.getSortOrder())
                    .createdAt(Instant.now())
                    .build();
            productImageRepository.save(image);

            if (product.getPrimaryImageUrl() == null || request.getSortOrder() == 0) {
                product.setPrimaryImageUrl(publicUrl);
                productRepository.save(product);
            }

            return ProductImageAttachResponse.builder()
                    .imageId(image.getId().toString())
                    .url(publicUrl)
                    .sortOrder(image.getSortOrder())
                    .alt(image.getAltText())
                    .build();
        } catch (RuntimeException ex) {
            log.warn("Product image confirm failed for upload {}: {}", uploadId, ex.getMessage());
            mediaAssetCleanupService.orphanAndDelete(asset);
            throw ex;
        }
    }

    @Transactional
    public void abortImageUpload(UUID userId, UUID productId, UUID uploadId) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        productAccessService.requireOwnedProduct(seller, productId);

        MediaAsset asset = mediaAssetRepository.findByIdAndUploadedByIdAndStatus(uploadId, userId, "UPLOADED")
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Pending upload not found"));
        mediaAssetCleanupService.orphanAndDelete(asset);
    }

    @Transactional
    public DuplicateProductResponse duplicateProduct(UUID userId, UUID productId) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Product source = productAccessService.requireOwnedProduct(seller, productId);

        if (ProductStatus.ARCHIVED.equals(source.getStatus())) {
            throw new ClosiqException(ErrorCode.INVALID_STATE_TRANSITION, "Cannot duplicate an archived product");
        }

        String title = source.getTitle() + " Copy";
        String slug = slugGenerator.uniqueSlug(title, productRepository::existsBySlugAndDeletedAtIsNull);
        UUID newProductId = IdGenerator.uuidV7();
        Instant now = Instant.now();

        Product duplicate = Product.builder()
                .id(newProductId)
                .productCode(productCodeGenerator.nextCode())
                .slug(slug)
                .title(title)
                .description(source.getDescription())
                .category(source.getCategory())
                .brand(source.getBrand())
                .pricePerDay(source.getPricePerDay())
                .depositAmount(source.getDepositAmount())
                .currencyCode(source.getCurrencyCode())
                .sellerProfileId(seller.getId())
                .city(source.getCity())
                .audience(source.getAudience())
                .garmentType(source.getGarmentType())
                .minRentalDays(source.getMinRentalDays())
                .maxRentalDays(source.getMaxRentalDays())
                .cleaningBufferDays(source.getCleaningBufferDays())
                .includesTrial(source.isIncludesTrial())
                .trialDurationMinutes(source.getTrialDurationMinutes())
                .featured(false)
                .trending(false)
                .status(ProductStatus.DRAFT)
                .reviewCount(0)
                .primaryImageUrl(source.getPrimaryImageUrl())
                .build();
        duplicate.setCreatedBy(userId);
        duplicate.setUpdatedBy(userId);
        productRepository.save(duplicate);

        for (ProductImage sourceImage : productImageRepository.findByProductIdOrderBySortOrderAsc(source.getId())) {
            productImageRepository.save(ProductImage.builder()
                    .id(IdGenerator.uuidV7())
                    .product(duplicate)
                    .imageUrl(sourceImage.getImageUrl())
                    .altText(sourceImage.getAltText())
                    .sortOrder(sourceImage.getSortOrder())
                    .createdAt(now)
                    .build());
        }

        short sort = 0;
        for (ProductVariant sourceVariant :
                productVariantRepository.findByProductIdOrderBySortOrderAsc(source.getId())) {
            ProductVariant variant = ProductVariant.builder()
                    .id(IdGenerator.uuidV7())
                    .product(duplicate)
                    .sku(buildSku(slug, sourceVariant.getVariantLabel()))
                    .variantLabel(sourceVariant.getVariantLabel())
                    .status(ACTIVE_VARIANT)
                    .sortOrder(++sort)
                    .createdAt(now)
                    .build();
            productVariantRepository.save(variant);
            int availableUnits = inventoryStockService.countAvailableUnits(sourceVariant.getId());
            createInventoryUnits(variant, Math.max(availableUnits, 1), userId);
        }

        return DuplicateProductResponse.builder()
                .productId(duplicate.getId().toString())
                .slug(duplicate.getSlug())
                .productCode(duplicate.getProductCode())
                .title(duplicate.getTitle())
                .status(duplicate.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse previewProduct(UUID userId, UUID productId) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Product product = productAccessService.requireOwnedProduct(seller, productId);
        return buildProductDetail(product);
    }

    @Transactional
    public void deleteImage(UUID userId, UUID productId, UUID imageId) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Product product = productAccessService.requireOwnedProduct(seller, productId);

        ProductImage image = productImageRepository.findByIdAndProduct_Id(imageId, product.getId())
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Image not found"));

        long imageCount = productImageRepository.countByProductId(product.getId());
        if (ProductStatus.ACTIVE.equals(product.getStatus()) && imageCount <= 1) {
            throw new ClosiqException(
                    ErrorCode.INVALID_STATE_TRANSITION, "Cannot delete the last image on an active listing");
        }

        productImageRepository.delete(image);

        if (publicUrlEquals(product.getPrimaryImageUrl(), image.getImageUrl())) {
            productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId()).stream()
                    .findFirst()
                    .ifPresentOrElse(
                            next -> product.setPrimaryImageUrl(next.getImageUrl()),
                            () -> product.setPrimaryImageUrl(null));
            productRepository.save(product);
        }
    }

    @Transactional(readOnly = true)
    public PagedResult<SellerProductListItemResponse> listProducts(
            UUID userId, String status, String pageToken, Integer limit) {

        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        int pageSize = normalizeLimit(limit);
        PageBoundary boundary = PageTokenCodec.productBoundary(pageToken);
        Specification<Product> spec = sellerProductsSpec(seller.getId(), status, boundary);

        List<Product> products = productRepository.findAll(
                spec,
                PageRequest.of(0, pageSize + 1, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .getContent();

        boolean hasMore = products.size() > pageSize;
        List<Product> pageItems = hasMore ? products.subList(0, pageSize) : products;

        List<SellerProductListItemResponse> items = pageItems.stream().map(this::toListItem).toList();
        String nextPageToken = null;
        if (hasMore && !pageItems.isEmpty()) {
            Product last = pageItems.get(pageItems.size() - 1);
            nextPageToken = PageTokenCodec.encodeProduct(new PageTokenCodec.ProductPageToken(last.getCreatedAt(), last.getId()));
        }

        return PagedResult.of(items, pageSize, hasMore, nextPageToken);
    }

    @Transactional(readOnly = true)
    public SellerProductDetailResponse getProduct(UUID userId, UUID productId) {
        SellerProfile seller = sellerContextService.requireVerifiedSeller(userId);
        Product product = productAccessService.requireOwnedProduct(seller, productId);

        List<ProductImage> productImages =
                productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        List<String> imageUrls = productImages.stream().map(ProductImage::getImageUrl).toList();
        if (imageUrls.isEmpty() && product.getPrimaryImageUrl() != null) {
            imageUrls = List.of(product.getPrimaryImageUrl());
        }

        List<SellerProductDetailResponse.ImageSummary> images = productImages.stream()
                .map(image -> SellerProductDetailResponse.ImageSummary.builder()
                        .id(image.getId().toString())
                        .url(image.getImageUrl())
                        .sortOrder(image.getSortOrder())
                        .build())
                .toList();

        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderBySortOrderAsc(product.getId());

        return SellerProductDetailResponse.builder()
                .id(product.getId().toString())
                .productCode(product.getProductCode())
                .slug(product.getSlug())
                .title(product.getTitle())
                .description(product.getDescription())
                .status(product.getStatus())
                .pricePerDay(product.getPricePerDay())
                .deposit(product.getDepositAmount())
                .city(product.getCity())
                .primaryImageUrl(product.getPrimaryImageUrl())
                .imageUrls(imageUrls)
                .images(images)
                .variants(variants.stream()
                        .map(variant -> SellerProductDetailResponse.VariantSummary.builder()
                                .id(variant.getId().toString())
                                .size(variant.getVariantLabel())
                                .status(variant.getStatus())
                                .availableQuantity(inventoryStockService.countAvailableUnits(variant.getId()))
                                .build())
                        .toList())
                .categoryId(product.getCategory() != null ? product.getCategory().getId().toString() : null)
                .occasion(product.getCategory() != null ? product.getCategory().getSlug() : null)
                .audience(product.getAudience())
                .garmentType(product.getGarmentType())
                .minRentalDays(product.getMinRentalDays())
                .maxRentalDays(product.getMaxRentalDays())
                .includesTrial(product.isIncludesTrial())
                .createdAt(product.getCreatedAt())
                .publishedAt(product.getPublishedAt())
                .build();
    }

    private Specification<Product> sellerProductsSpec(
            UUID sellerProfileId, String status, PageBoundary boundary) {

        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.or(
                    cb.isNull(root.get("deletedAt")),
                    cb.equal(root.get("status"), ProductStatus.ARCHIVED)));
            predicates.add(cb.equal(root.get("sellerProfileId"), sellerProfileId));
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.toUpperCase(Locale.ROOT)));
            }
            predicates.add(cb.or(
                    cb.lessThan(root.get("createdAt"), boundary.beforeCreatedAt()),
                    cb.and(
                            cb.equal(root.get("createdAt"), boundary.beforeCreatedAt()),
                            cb.lessThan(root.get("id"), boundary.beforeId()))));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Category resolveCategory(UUID categoryId, String occasion) {
        Category category = categoryRepository.findByIdAndStatus(categoryId, ACTIVE_CATEGORY)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Category not found"));

        if (occasion != null && !occasion.equalsIgnoreCase(category.getSlug())) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Occasion does not match category");
        }
        return category;
    }

    private Brand resolveBrand(String designer) {
        if (designer == null || designer.isBlank()) {
            return null;
        }
        String slug = SlugUtils.slugify(designer);
        return brandRepository.findBySlug(slug).orElseGet(() -> {
            Brand brand = Brand.builder()
                    .id(IdGenerator.uuidV7())
                    .slug(slug)
                    .name(designer.trim())
                    .status(ACTIVE_BRAND)
                    .createdAt(Instant.now())
                    .build();
            return brandRepository.save(brand);
        });
    }

    private void validatePricing(long pricePerDay, long deposit) {
        if (deposit < pricePerDay) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Deposit must be at least the daily rental price");
        }
    }

    private boolean hasActiveBookings(UUID productId) {
        return bookingItemRepository.countActiveFutureBookingsForProduct(productId, LocalDate.now()) > 0;
    }

    private void createInventoryUnits(ProductVariant variant, int quantity, UUID actorId) {
        for (int i = 0; i < quantity; i++) {
            String serial = variant.getSku() + "-" + IdGenerator.uuidV7().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            InventoryItem item = InventoryItem.builder()
                    .id(IdGenerator.uuidV7())
                    .productVariant(variant)
                    .serialNumber(serial)
                    .conditionGrade("EXCELLENT")
                    .status(InventoryItemStatus.AVAILABLE)
                    .acquiredAt(Instant.now())
                    .build();
            inventoryItemRepository.save(item);
            inventoryHistoryService.recordItemCreated(item, actorId);
        }
    }

    private String buildSku(String productSlug, String size) {
        return productSlug + "-" + SlugUtils.slugify(size);
    }

    private boolean matchesCreateRequest(Product product, CreateSellerProductRequest request) {
        return product.getTitle().equals(request.getTitle())
                && product.getPricePerDay() == request.getPricePerDay()
                && product.getDepositAmount() == request.getDeposit();
    }

    private SellerProductResponse toProductResponse(Product product) {
        return SellerProductResponse.builder()
                .id(product.getId())
                .slug(product.getSlug())
                .productCode(product.getProductCode())
                .title(product.getTitle())
                .status(product.getStatus())
                .pricePerDay(product.getPricePerDay())
                .deposit(product.getDepositAmount())
                .city(product.getCity())
                .build();
    }

    private SellerProductListItemResponse toListItem(Product product) {
        return SellerProductListItemResponse.builder()
                .id(product.getId().toString())
                .productCode(product.getProductCode())
                .slug(product.getSlug())
                .title(product.getTitle())
                .status(product.getStatus())
                .pricePerDay(product.getPricePerDay())
                .deposit(product.getDepositAmount())
                .primaryImageUrl(product.getPrimaryImageUrl())
                .createdAt(product.getCreatedAt())
                .publishedAt(product.getPublishedAt())
                .build();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        return Math.min(Math.max(limit, 1), 50);
    }

    private String extensionFor(String contentType, String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.'));
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private boolean publicUrlEquals(String a, String b) {
        return a != null && a.equals(b);
    }

    private void validateImageContentType(String contentType) {
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Only JPEG, PNG, and WebP images are supported");
        }
    }

    private ProductDetailResponse buildProductDetail(Product product) {
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        SellerProfile seller = product.getSellerProfileId() != null
                ? sellerProfileRepository.findById(product.getSellerProfileId()).orElse(null)
                : null;

        Map<UUID, Brand> brands = new HashMap<>();
        if (product.getBrand() != null) {
            brands.put(product.getBrand().getId(), product.getBrand());
        }
        Map<UUID, SellerProfile> sellers = seller != null ? Map.of(seller.getId(), seller) : Map.of();
        Map<UUID, List<ProductImage>> imagesByProduct = Map.of(product.getId(), images);

        String designer = productMapper.resolveDesigner(product, brands, sellers);
        List<String> imageUrls = productMapper.resolveImageUrls(product, imagesByProduct);

        List<UUID> variantIds = variants.stream().map(ProductVariant::getId).toList();
        Map<UUID, Integer> unitsByVariant = inventoryStockService.countAvailableUnitsByVariant(variantIds);
        int totalStock = unitsByVariant.values().stream().mapToInt(Integer::intValue).sum();

        return productMapper.toDetail(product, designer, imageUrls, variants, seller, unitsByVariant, totalStock);
    }
}
