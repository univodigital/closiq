package com.closiq.admin.service;

import com.closiq.admin.web.dto.AdminProductListItemResponse;
import com.closiq.admin.web.dto.UpdateAdminProductRequest;
import com.closiq.catalog.domain.Product;
import com.closiq.catalog.domain.ProductStatus;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.web.PageBoundary;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final ProductRepository productRepository;
    private final SellerProfileRepository sellerProfileRepository;

    @Transactional(readOnly = true)
    public PagedResult<AdminProductListItemResponse> listProducts(String status, String pageToken, Integer limit) {
        int pageSize = clampLimit(limit);
        PageBoundary boundary = PageTokenCodec.productBoundary(pageToken);

        Specification<Product> spec = Specification.where(notDeleted())
                .and(boundary.createdBefore());
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        List<Product> products = productRepository.findAll(
                spec,
                PageRequest.of(0, pageSize + 1, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .getContent();

        boolean hasMore = products.size() > pageSize;
        List<Product> page = hasMore ? products.subList(0, pageSize) : products;

        Map<UUID, String> sellerNames = loadSellerNames(page);
        List<AdminProductListItemResponse> items = page.stream()
                .map(product -> toListItem(product, sellerNames))
                .toList();

        String nextPageToken = null;
        if (hasMore && !page.isEmpty()) {
            Product last = page.get(page.size() - 1);
            nextPageToken = PageTokenCodec.encodeProduct(new PageTokenCodec.ProductPageToken(last.getCreatedAt(), last.getId()));
        }

        return PagedResult.of(items, pageSize, hasMore, nextPageToken);
    }

    @Transactional
    public void deleteProduct(UUID productId, UUID adminId) {
        Product product = requireProduct(productId);
        product.setStatus(ProductStatus.ARCHIVED);
        product.setDeletedAt(Instant.now());
        product.setUpdatedBy(adminId);
        productRepository.save(product);
    }

    @Transactional
    public AdminProductListItemResponse updateProduct(UUID productId, UpdateAdminProductRequest request, UUID adminId) {
        Product product = requireProduct(productId);

        if (request.getStatus() != null) {
            validateProductStatus(request.getStatus());
            product.setStatus(request.getStatus());
            if (ProductStatus.ACTIVE.equals(request.getStatus()) && product.getPublishedAt() == null) {
                product.setPublishedAt(Instant.now());
            }
            product.setUpdatedBy(adminId);
            productRepository.save(product);
        }

        Map<UUID, String> sellerNames = loadSellerNames(List.of(product));
        return toListItem(product, sellerNames);
    }

    private AdminProductListItemResponse toListItem(Product product, Map<UUID, String> sellerNames) {
        String sellerName = product.getSellerProfileId() != null
                ? sellerNames.getOrDefault(product.getSellerProfileId(), "Unknown seller")
                : null;

        return AdminProductListItemResponse.builder()
                .id(product.getId().toString())
                .productCode(product.getProductCode())
                .slug(product.getSlug())
                .title(product.getTitle())
                .status(product.getStatus())
                .sellerBusinessName(sellerName)
                .primaryImageUrl(product.getPrimaryImageUrl())
                .pricePerDay(product.getPricePerDay())
                .createdAt(product.getCreatedAt())
                .publishedAt(product.getPublishedAt())
                .build();
    }

    private Map<UUID, String> loadSellerNames(List<Product> products) {
        List<UUID> sellerIds = products.stream()
                .map(Product::getSellerProfileId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (sellerIds.isEmpty()) {
            return Map.of();
        }

        return sellerProfileRepository.findAllById(sellerIds).stream()
                .collect(Collectors.toMap(SellerProfile::getId, SellerProfile::getBusinessName));
    }

    private Product requireProduct(UUID productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Product not found"));
    }

    private Specification<Product> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private void validateProductStatus(String status) {
        if (!List.of(ProductStatus.DRAFT, ProductStatus.PENDING_REVIEW, ProductStatus.ACTIVE, ProductStatus.ARCHIVED)
                .contains(status)) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Invalid product status");
        }
    }

    private int clampLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
