package com.closiq.catalog.mapper;

import com.closiq.catalog.domain.Brand;
import com.closiq.catalog.domain.Category;
import com.closiq.catalog.domain.Product;
import com.closiq.catalog.domain.ProductImage;
import com.closiq.catalog.domain.ProductVariant;
import com.closiq.catalog.domain.PromotionalOffer;
import com.closiq.catalog.web.dto.CategoryResponse;
import com.closiq.catalog.web.dto.OfferResponse;
import com.closiq.catalog.web.dto.ProductDetailResponse;
import com.closiq.catalog.web.dto.ProductImageResponse;
import com.closiq.catalog.web.dto.ProductSummaryResponse;
import com.closiq.catalog.web.dto.ProductVariantResponse;
import com.closiq.inventory.service.InventoryStockService;
import com.closiq.user.domain.SellerProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final InventoryStockService inventoryStockService;

    public ProductSummaryResponse toSummary(
            Product product, String designer, List<String> imageUrls, int totalAvailableUnits) {
        return ProductSummaryResponse.builder()
                .id(product.getId().toString())
                .slug(product.getSlug())
                .productCode(product.getProductCode())
                .title(product.getTitle())
                .designer(designer)
                .images(imageUrls)
                .pricePerDay(product.getPricePerDay())
                .deposit(product.getDepositAmount())
                .currency(product.getCurrencyCode())
                .rating(product.getAvgRating() != null ? product.getAvgRating().doubleValue() : null)
                .reviewCount(product.getReviewCount())
                .badges(buildBadges(product, totalAvailableUnits))
                .includesTrial(product.isIncludesTrial())
                .city(product.getCity())
                .audience(product.getAudience())
                .garmentType(product.getGarmentType())
                .trending(product.isTrending())
                .build();
    }

    public ProductSummaryResponse toSummary(Product product, String designer, List<String> imageUrls) {
        return toSummary(product, designer, imageUrls, 0);
    }

    public ProductDetailResponse toDetail(
            Product product,
            String designer,
            List<String> imageUrls,
            List<ProductVariant> variants,
            SellerProfile seller,
            Map<UUID, Integer> unitsByVariant,
            int totalAvailableUnits) {

        Category category = product.getCategory();
        return ProductDetailResponse.builder()
                .id(product.getId().toString())
                .slug(product.getSlug())
                .productCode(product.getProductCode())
                .title(product.getTitle())
                .designer(designer)
                .description(product.getDescription())
                .categoryId(category != null ? category.getId().toString() : null)
                .occasion(category != null ? category.getSlug() : null)
                .images(imageUrls)
                .pricePerDay(product.getPricePerDay())
                .deposit(product.getDepositAmount())
                .currency(product.getCurrencyCode())
                .variants(variants.stream()
                        .map(v -> toVariant(v, unitsByVariant.getOrDefault(v.getId(), 0)))
                        .toList())
                .rating(product.getAvgRating() != null ? product.getAvgRating().doubleValue() : null)
                .reviewCount(product.getReviewCount())
                .badges(buildBadges(product, totalAvailableUnits))
                .sellerId(seller != null ? seller.getId().toString() : null)
                .sellerName(seller != null ? seller.getBusinessName() : designer)
                .city(product.getCity())
                .deliverablePincodes(List.of())
                .includesTrial(product.isIncludesTrial())
                .trialDurationMinutes(product.getTrialDurationMinutes())
                .minRentalDays(product.getMinRentalDays())
                .maxRentalDays(product.getMaxRentalDays())
                .sizeGuide(null)
                .audience(product.getAudience())
                .garmentType(product.getGarmentType())
                .trending(product.isTrending())
                .build();
    }

    public ProductVariantResponse toVariant(ProductVariant variant, int availableUnits) {
        return ProductVariantResponse.builder()
                .id(variant.getId().toString())
                .size(variant.getVariantLabel())
                .available("ACTIVE".equals(variant.getStatus()) && availableUnits > 0)
                .build();
    }

    public ProductVariantResponse toVariant(ProductVariant variant) {
        return toVariant(variant, 0);
    }

    public CategoryResponse toCategory(Category category, long productCount) {
        return CategoryResponse.builder()
                .id(category.getId().toString())
                .slug(category.getSlug())
                .name(category.getName())
                .description(category.getDescription())
                .image(category.getImageUrl())
                .productCount(productCount)
                .featured(category.isFeatured())
                .sortOrder(category.getSortOrder())
                .build();
    }

    public OfferResponse toOffer(PromotionalOffer offer) {
        return OfferResponse.builder()
                .id(offer.getId().toString())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .code(offer.getCode())
                .discountType(offer.getDiscountType())
                .discountValue(offer.getDiscountValue())
                .validUntil(offer.getValidUntil())
                .imageUrl(offer.getImageUrl())
                .build();
    }

    public ProductImageResponse toImage(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId().toString())
                .url(image.getImageUrl())
                .sortOrder(image.getSortOrder())
                .alt(image.getAltText())
                .build();
    }

    public String resolveDesigner(Product product, Map<UUID, Brand> brands, Map<UUID, SellerProfile> sellers) {
        if (product.getBrand() != null) {
            Brand brand = brands.get(product.getBrand().getId());
            if (brand != null) {
                return brand.getName();
            }
            return product.getBrand().getName();
        }
        if (product.getSellerProfileId() != null) {
            SellerProfile seller = sellers.get(product.getSellerProfileId());
            if (seller != null) {
                return seller.getBusinessName();
            }
        }
        return product.getTitle();
    }

    public List<String> resolveImageUrls(Product product, Map<UUID, List<ProductImage>> imagesByProduct) {
        List<ProductImage> images = imagesByProduct.get(product.getId());
        if (images != null && !images.isEmpty()) {
            return images.stream().map(ProductImage::getImageUrl).toList();
        }
        if (product.getPrimaryImageUrl() != null) {
            return List.of(product.getPrimaryImageUrl());
        }
        return List.of();
    }

    private List<String> buildBadges(Product product, int totalAvailableUnits) {
        List<String> badges = new ArrayList<>();
        if (product.isIncludesTrial()) {
            badges.add("INCLUDES_TRIAL");
        }
        if (inventoryStockService.isLowStock(totalAvailableUnits)) {
            badges.add("LOW_STOCK");
        }
        return badges;
    }
}
