package com.closiq.user.mapper;

import com.closiq.catalog.domain.Product;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.common.security.RoleType;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserProfile;
import com.closiq.user.domain.Address;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.domain.WishlistItem;
import com.closiq.user.service.UserPreferencesHelper;
import com.closiq.user.web.dto.AddressResponse;
import com.closiq.user.web.dto.NotificationPreferencesResponse;
import com.closiq.user.web.dto.ProductSummaryResponse;
import com.closiq.user.web.dto.SellerProfileResponse;
import com.closiq.user.web.dto.UserPreferencesResponse;
import com.closiq.user.web.dto.UserProfileResponse;
import com.closiq.user.web.dto.WishlistItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class UserProfileMapper {

    private static final String ACTIVE_PRODUCT_STATUS = "ACTIVE";

    @Autowired
    protected UserPreferencesHelper preferencesHelper;

    @Autowired
    protected ProductRepository productRepository;

    @Mapping(target = "id", source = "user.id", qualifiedByName = "uuidToString")
    @Mapping(target = "userCode", source = "user.userCode")
    @Mapping(target = "phone", source = "user.phone")
    @Mapping(target = "phoneVerified", source = "user.phoneVerified")
    @Mapping(target = "alternatePhone", source = "user.alternatePhone")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "emailVerified", source = "user.emailVerified")
    @Mapping(target = "alternateEmail", source = "user.alternateEmail")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "createdAt", source = "user.createdAt")
    @Mapping(target = "avatarUrl", expression = "java(preferencesHelper.getAvatarUrl(profile.getPreferences()))")
    @Mapping(target = "preferences", expression = "java(toPreferencesResponse(profile.getPreferences()))")
    @Mapping(target = "sellerProfile", source = "sellerProfile")
    public abstract UserProfileResponse toProfileResponse(
            User user,
            UserProfile profile,
            List<String> roles,
            SellerProfileResponse sellerProfile);

    public UserProfileResponse toProfileResponse(
            User user, UserProfile profile, List<RoleType> roles, SellerProfile sellerProfile) {
        SellerProfileResponse seller = sellerProfile == null
                ? null
                : toSellerResponse(sellerProfile, countActiveListings(sellerProfile));
        List<String> roleNames = roles.stream().map(Enum::name).toList();
        return toProfileResponse(user, profile, roleNames, seller);
    }

    @Mapping(target = "sellerId", source = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "verificationStatus", source = "status", qualifiedByName = "mapVerificationStatus")
    @Mapping(target = "rating", source = "avgRating")
    @Mapping(target = "listingCount", ignore = true)
    public abstract SellerProfileResponse toSellerResponse(SellerProfile sellerProfile);

    public SellerProfileResponse toSellerResponse(SellerProfile sellerProfile, long listingCount) {
        if (sellerProfile == null) {
            return null;
        }
        SellerProfileResponse base = toSellerResponse(sellerProfile);
        return SellerProfileResponse.builder()
                .sellerId(base.getSellerId())
                .businessName(base.getBusinessName())
                .verificationStatus(base.getVerificationStatus())
                .city(base.getCity())
                .rating(base.getRating())
                .listingCount(listingCount)
                .build();
    }

    protected long countActiveListings(SellerProfile sellerProfile) {
        if (sellerProfile == null || sellerProfile.getId() == null) {
            return 0L;
        }
        return productRepository.countBySellerProfileIdAndStatusAndDeletedAtIsNull(
                sellerProfile.getId(), ACTIVE_PRODUCT_STATUS);
    }

    @Named("mapVerificationStatus")
    protected String mapVerificationStatus(String status) {
        if ("ACTIVE".equals(status)) {
            return "VERIFIED";
        }
        return status;
    }

    public UserPreferencesResponse toPreferencesResponse(java.util.Map<String, Object> preferences) {
        var shopping = preferencesHelper.getShopping(preferences);
        return UserPreferencesResponse.builder()
                .size(shopping.size())
                .occasions(shopping.occasions())
                .build();
    }

    public NotificationPreferencesResponse toNotificationResponse(java.util.Map<String, Object> preferences) {
        var notif = preferencesHelper.getNotifications(preferences);
        return NotificationPreferencesResponse.builder()
                .emailEnabled(notif.emailEnabled())
                .smsEnabled(notif.smsEnabled())
                .pushEnabled(notif.pushEnabled())
                .orderUpdates(notif.orderUpdates())
                .promotions(notif.promotions())
                .sellerBookingAlerts(notif.sellerBookingAlerts())
                .build();
    }

    @Mapping(target = "id", source = "address.id", qualifiedByName = "uuidToString")
    @Mapping(target = "isDefault", expression = "java(address.isDefault())")
    @Mapping(target = "serviceable", source = "serviceable")
    public abstract AddressResponse toAddressResponse(Address address, boolean serviceable);

    @Mapping(target = "productId", source = "item.id.productId", qualifiedByName = "uuidToString")
    @Mapping(target = "addedAt", source = "item.createdAt")
    @Mapping(target = "product", source = "item.product")
    public abstract WishlistItemResponse toWishlistResponse(WishlistItem item);

    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "deposit", source = "depositAmount")
    @Mapping(target = "imageUrl", source = "primaryImageUrl")
    public abstract ProductSummaryResponse toProductSummary(Product product);

    @Named("uuidToString")
    protected String uuidToString(UUID id) {
        return id != null ? id.toString() : null;
    }
}
