package com.closiq.catalog.domain;

import com.closiq.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "product_code", nullable = false, unique = true, length = 32)
    private String productCode;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "price_per_day", nullable = false)
    private long pricePerDay;

    @Column(name = "deposit_amount", nullable = false)
    private long depositAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "primary_image_url", length = 512)
    private String primaryImageUrl;

    @Column(name = "seller_profile_id")
    private UUID sellerProfileId;

    @Column(name = "min_rental_days", nullable = false)
    private short minRentalDays;

    @Column(name = "max_rental_days")
    private Short maxRentalDays;

    @Column(name = "cleaning_buffer_days", nullable = false)
    private short cleaningBufferDays;

    @Column(name = "includes_trial", nullable = false)
    private boolean includesTrial;

    @Column(name = "trial_duration_minutes", nullable = false)
    private short trialDurationMinutes;

    @Column(length = 50)
    private String city;

    @Column(name = "avg_rating", precision = 2, scale = 1)
    private BigDecimal avgRating;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean trending;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 10)
    private String audience;

    @Column(name = "garment_type", length = 50)
    private String garmentType;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();
}
