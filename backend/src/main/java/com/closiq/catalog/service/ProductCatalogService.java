package com.closiq.catalog.service;

import com.closiq.catalog.domain.Brand;
import com.closiq.catalog.domain.Category;
import com.closiq.catalog.domain.Product;
import com.closiq.catalog.domain.ProductImage;
import com.closiq.catalog.domain.ProductVariant;
import com.closiq.catalog.mapper.ProductMapper;
import com.closiq.catalog.repository.CategoryRepository;
import com.closiq.catalog.repository.ProductImageRepository;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.catalog.repository.ProductVariantRepository;
import com.closiq.catalog.web.dto.ProductFiltersResponse;
import com.closiq.catalog.web.dto.ProductSummaryResponse;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.web.PageBoundary;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.inventory.service.AvailabilityService;
import com.closiq.inventory.service.InventoryStockService;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCatalogService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final String ACTIVE = ProductSpecifications.ACTIVE;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final ProductMapper productMapper;
    private final InventoryStockService inventoryStockService;
    private final AvailabilityService availabilityService;

    @Transactional(readOnly = true)
    public PagedResult<ProductSummaryResponse> listProducts(
            String occasion,
            UUID categoryId,
            String size,
            Long minPrice,
            Long maxPrice,
            String city,
            Boolean featured,
            Boolean trending,
            String audience,
            String garmentType,
            String sort,
            String pageToken,
            Integer limit,
            LocalDate rentalStartDate,
            LocalDate rentalEndDate) {

        return queryProducts(
                occasion, categoryId, size, minPrice, maxPrice, city, featured, trending, audience, garmentType,
                null, sort, pageToken, limit, rentalStartDate, rentalEndDate);
    }

    /** @deprecated use overload with rental dates */
    @Transactional(readOnly = true)
    public PagedResult<ProductSummaryResponse> listProducts(
            String occasion,
            UUID categoryId,
            String size,
            Long minPrice,
            Long maxPrice,
            String city,
            Boolean featured,
            Boolean trending,
            String audience,
            String garmentType,
            String sort,
            String pageToken,
            Integer limit) {
        return listProducts(
                occasion, categoryId, size, minPrice, maxPrice, city, featured, trending, audience, garmentType,
                sort, pageToken, limit, null, null);
    }

    @Transactional(readOnly = true)
    public SearchResult searchProducts(
            String query,
            String occasion,
            String size,
            Long minPrice,
            Long maxPrice,
            String sort,
            String pageToken,
            Integer limit,
            LocalDate rentalStartDate,
            LocalDate rentalEndDate) {

        if (query == null || query.trim().length() < 2) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Search query must be at least 2 characters");
        }

        long start = System.currentTimeMillis();
        PagedResult<ProductSummaryResponse> page = queryProducts(
                occasion, null, size, minPrice, maxPrice, null, null, null, null, null, query.trim(), sort,
                pageToken, limit, rentalStartDate, rentalEndDate);
        long tookMs = System.currentTimeMillis() - start;

        PageBoundary countBoundary = PageBoundary.now();
        long totalCount = productRepository.count(ProductSpecifications.combine(buildFilter(
                occasion, null, size, minPrice, maxPrice, null, null, null, null, null, query.trim(),
                countBoundary.beforeCreatedAt(), countBoundary.beforeId())));

        return new SearchResult(page, query.trim(), totalCount, tookMs);
    }

    @Transactional(readOnly = true)
    public com.closiq.catalog.web.dto.ProductDetailResponse getProductBySlugOrId(String slugOrId) {
        Product product = productRepository.findActiveBySlugOrId(slugOrId, ACTIVE)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Product not found"));

        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        SellerProfile seller = product.getSellerProfileId() != null
                ? sellerProfileRepository.findById(product.getSellerProfileId()).orElse(null)
                : null;

        Map<UUID, Brand> brands = new HashMap<>();
        if (product.getBrand() != null) {
            brands.put(product.getBrand().getId(), product.getBrand());
        }
        Map<UUID, SellerProfile> sellers = seller != null
                ? Map.of(seller.getId(), seller)
                : Map.of();
        Map<UUID, List<ProductImage>> imagesByProduct = Map.of(product.getId(), images);

        String designer = productMapper.resolveDesigner(product, brands, sellers);
        List<String> imageUrls = productMapper.resolveImageUrls(product, imagesByProduct);

        List<UUID> variantIds = variants.stream().map(ProductVariant::getId).toList();
        Map<UUID, Integer> unitsByVariant = inventoryStockService.countAvailableUnitsByVariant(variantIds);
        int totalStock = unitsByVariant.values().stream().mapToInt(Integer::intValue).sum();

        return productMapper.toDetail(product, designer, imageUrls, variants, seller, unitsByVariant, totalStock);
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getRelatedProducts(String slugOrId, int limit) {
        Product product = productRepository.findActiveBySlugOrId(slugOrId, ACTIVE)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Product not found"));

        if (product.getCategory() == null) {
            return List.of();
        }

        int pageSize = Math.min(Math.max(limit, 1), 12);
        List<Product> related = productRepository.findByCategoryIdAndIdNotAndDeletedAtIsNullAndStatusOrderByAvgRatingDesc(
                product.getCategory().getId(), product.getId(), ACTIVE, PageRequest.of(0, pageSize));

        return toSummaries(related);
    }

    @Transactional(readOnly = true)
    public ProductFiltersResponse getFilters(String occasion, String query) {
        List<Category> categories = categoryRepository.findByStatusOrderBySortOrderAsc(ACTIVE);
        List<ProductFiltersResponse.FacetOption> occasionFacets = categories.stream()
                .map(cat -> ProductFiltersResponse.FacetOption.builder()
                        .slug(cat.getSlug())
                        .name(cat.getName())
                        .count(productRepository.countByCategoryIdAndDeletedAtIsNullAndStatus(cat.getId(), ACTIVE))
                        .build())
                .filter(f -> f.getCount() > 0)
                .toList();

        List<Product> activeProducts = productRepository.findAll(ProductSpecifications.activeOnly());
        long minPrice = activeProducts.stream().mapToLong(Product::getPricePerDay).min().orElse(0);
        long maxPrice = activeProducts.stream().mapToLong(Product::getPricePerDay).max().orElse(0);

        Map<String, Long> sizeCounts = new HashMap<>();
        Map<String, Long> cityCounts = new HashMap<>();
        for (Product product : activeProducts) {
            if (product.getCity() != null) {
                cityCounts.merge(product.getCity(), 1L, Long::sum);
            }
            productVariantRepository.findByProductIdOrderBySortOrderAsc(product.getId()).stream()
                    .filter(v -> ACTIVE.equals(v.getStatus()))
                    .forEach(v -> sizeCounts.merge(v.getVariantLabel(), 1L, Long::sum));
        }

        return ProductFiltersResponse.builder()
                .occasions(occasionFacets)
                .sizes(sizeCounts.entrySet().stream()
                        .map(e -> ProductFiltersResponse.FacetOption.builder()
                                .value(e.getKey())
                                .count(e.getValue())
                                .build())
                        .toList())
                .priceRange(ProductFiltersResponse.PriceRange.builder().min(minPrice).max(maxPrice).build())
                .cities(cityCounts.entrySet().stream()
                        .map(e -> ProductFiltersResponse.FacetOption.builder()
                                .value(e.getKey())
                                .count(e.getValue())
                                .build())
                        .toList())
                .build();
    }

    private PagedResult<ProductSummaryResponse> queryProducts(
            String occasion,
            UUID categoryId,
            String size,
            Long minPrice,
            Long maxPrice,
            String city,
            Boolean featured,
            Boolean trending,
            String audience,
            String garmentType,
            String query,
            String sort,
            String pageToken,
            Integer limit,
            LocalDate rentalStartDate,
            LocalDate rentalEndDate) {

        int pageSize = normalizeLimit(limit);
        PageBoundary boundary = PageTokenCodec.productBoundary(pageToken);

        ProductSpecifications.ProductFilter filter = buildFilter(
                occasion, categoryId, size, minPrice, maxPrice, city, featured, trending, audience, garmentType,
                query, boundary.beforeCreatedAt(), boundary.beforeId());

        Specification<Product> spec = ProductSpecifications.combine(filter);
        Sort sortOrder = parseSort(sort);

        List<Product> products = productRepository.findAll(spec, PageRequest.of(0, pageSize + 1, sortOrder))
                .getContent();

        boolean hasMore = products.size() > pageSize;
        List<Product> pageItems = hasMore ? products.subList(0, pageSize) : products;
        List<ProductSummaryResponse> summaries = toSummaries(pageItems, rentalStartDate, rentalEndDate);

        String nextPageToken = hasMore && !pageItems.isEmpty()
                ? PageTokenCodec.encodeProduct(new PageTokenCodec.ProductPageToken(
                        pageItems.getLast().getCreatedAt(),
                        pageItems.getLast().getId()))
                : null;

        return PagedResult.of(summaries, pageSize, hasMore, nextPageToken);
    }

    private List<ProductSummaryResponse> toSummaries(
            List<Product> products, LocalDate rentalStartDate, LocalDate rentalEndDate) {
        if (products.isEmpty()) {
            return List.of();
        }

        Map<UUID, Boolean> availabilityByProduct = Map.of();
        if (rentalStartDate != null && rentalEndDate != null) {
            availabilityByProduct = availabilityService.areProductsAvailableForDates(
                    products, rentalStartDate, rentalEndDate);
        }

        List<UUID> productIds = products.stream().map(Product::getId).toList();
        Map<UUID, List<ProductImage>> imagesByProduct = productImageRepository
                .findByProductIdInOrderBySortOrderAsc(productIds).stream()
                .collect(Collectors.groupingBy(img -> img.getProduct().getId()));

        Map<UUID, SellerProfile> sellers = sellerProfileRepository.findAllById(
                        products.stream()
                                .map(Product::getSellerProfileId)
                                .filter(id -> id != null)
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(SellerProfile::getId, s -> s));

        Map<UUID, Brand> brands = products.stream()
                .filter(p -> p.getBrand() != null)
                .collect(Collectors.toMap(p -> p.getBrand().getId(), Product::getBrand, (a, b) -> a));

        Map<UUID, Integer> stockByProduct = inventoryStockService.countAvailableUnitsByProduct(productIds);

        Map<UUID, Boolean> dateAvailability = availabilityByProduct;
        return products.stream()
                .map(product -> {
                    String designer = productMapper.resolveDesigner(product, brands, sellers);
                    List<String> images = productMapper.resolveImageUrls(product, imagesByProduct);
                    int stock = stockByProduct.getOrDefault(product.getId(), 0);
                    Boolean availableForDates = rentalStartDate != null && rentalEndDate != null
                            ? dateAvailability.getOrDefault(product.getId(), false)
                            : null;
                    return productMapper.toSummary(product, designer, images, stock, availableForDates);
                })
                .toList();
    }

    private List<ProductSummaryResponse> toSummaries(List<Product> products) {
        return toSummaries(products, null, null);
    }

    private ProductSpecifications.ProductFilter buildFilter(
            String occasion,
            UUID categoryId,
            String size,
            Long minPrice,
            Long maxPrice,
            String city,
            Boolean featured,
            Boolean trending,
            String audience,
            String garmentType,
            String query,
            java.time.Instant beforeCreatedAt,
            UUID beforeId) {

        List<String> occasionSlugs = occasion != null && !occasion.isBlank()
                ? Arrays.stream(occasion.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();
        List<String> sizes = size != null && !size.isBlank()
                ? Arrays.stream(size.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();

        return new ProductSpecifications.ProductFilter(
                occasionSlugs, categoryId, sizes, minPrice, maxPrice, city, featured, trending, audience,
                garmentType, query, beforeCreatedAt, beforeId);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        }
        String[] parts = sort.split(":");
        String field = parts[0];
        boolean asc = parts.length < 2 || !"desc".equalsIgnoreCase(parts[1]);
        return switch (field) {
            case "pricePerDay" -> Sort.by(asc ? Sort.Order.asc("pricePerDay") : Sort.Order.desc("pricePerDay"),
                    Sort.Order.desc("id"));
            case "rating" -> Sort.by(asc ? Sort.Order.asc("avgRating") : Sort.Order.desc("avgRating"),
                    Sort.Order.desc("id"));
            default -> Sort.by(asc ? Sort.Order.asc("createdAt") : Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id"));
        };
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    public record SearchResult(PagedResult<ProductSummaryResponse> page, String query, long totalCount, long tookMs) {
    }
}
