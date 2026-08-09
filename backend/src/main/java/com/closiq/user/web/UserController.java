package com.closiq.user.web;

import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.common.web.ClosiqRequestIdFilter;
import com.closiq.user.service.AddressService;
import com.closiq.user.service.UserAccountService;
import com.closiq.user.service.UserProfileService;
import com.closiq.user.service.UserSettingsService;
import com.closiq.user.service.WishlistService;
import com.closiq.user.web.dto.AddWishlistRequest;
import com.closiq.user.web.dto.AddressResponse;
import com.closiq.user.web.dto.AccountSettingsResponse;
import com.closiq.user.web.dto.CreateAddressRequest;
import com.closiq.user.web.dto.NotificationPreferencesResponse;
import com.closiq.user.web.dto.UpdateAccountSettingsRequest;
import com.closiq.user.web.dto.UpdateAddressRequest;
import com.closiq.user.web.dto.UpdateNotificationPreferencesRequest;
import com.closiq.user.web.dto.UpdateProfileRequest;
import com.closiq.user.web.dto.UserProfileResponse;
import com.closiq.user.web.dto.WishlistItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "User", description = "Profile, addresses, wishlist, and settings")
public class UserController {

    private final UserProfileService userProfileService;
    private final AddressService addressService;
    private final WishlistService wishlistService;
    private final UserSettingsService userSettingsService;
    private final UserAccountService userAccountService;

    @GetMapping
    @Operation(summary = "Get full user profile including preferences")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        UserProfileResponse profile = userProfileService.getProfile(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(profile, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PatchMapping
    @Operation(summary = "Update profile, email, avatar, or shopping preferences")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest body,
            HttpServletRequest request) {

        UserProfileResponse profile = userProfileService.updateProfile(principal.userId(), body);
        return ResponseEntity.ok(ApiResponse.ok(profile, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @DeleteMapping
    @Operation(summary = "Permanently delete the authenticated user's account")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UserPrincipal principal) {
        userAccountService.deleteAccount(principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/addresses")
    @Operation(summary = "List saved delivery addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> listAddresses(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        List<AddressResponse> addresses = addressService.listAddresses(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(addresses, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/addresses")
    @Operation(summary = "Add a delivery address")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAddressRequest body,
            HttpServletRequest request) {

        AddressResponse address = addressService.createAddress(principal.userId(), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(address, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PatchMapping("/addresses/{addressId}")
    @Operation(summary = "Update a delivery address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateAddressRequest body,
            HttpServletRequest request) {

        AddressResponse address = addressService.updateAddress(principal.userId(), addressId, body);
        return ResponseEntity.ok(ApiResponse.ok(address, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @DeleteMapping("/addresses/{addressId}")
    @Operation(summary = "Remove a delivery address")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId) {

        addressService.deleteAddress(principal.userId(), addressId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/wishlist")
    @Operation(summary = "List wishlisted products")
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> listWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            HttpServletRequest request) {

        PagedResult<WishlistItemResponse> page = wishlistService.listWishlist(principal.userId(), pageToken, limit);
        return ResponseEntity.ok(ApiResponse.ok(
                page.getItems(),
                ClosiqRequestIdFilter.getRequestId(request),
                PageTokenCodec.toPaginationMeta(page)));
    }

    @PostMapping("/wishlist")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<ApiResponse<WishlistItemResponse>> addToWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddWishlistRequest body,
            HttpServletRequest request) {

        WishlistItemResponse item = wishlistService.addToWishlist(
                principal.userId(), UUID.fromString(body.getProductId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(item, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @DeleteMapping("/wishlist/{productId}")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<Void> removeFromWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId) {

        wishlistService.removeFromWishlist(principal.userId(), productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings/notifications")
    @Operation(summary = "Get notification channel preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> getNotificationPreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        NotificationPreferencesResponse prefs = userSettingsService.getNotificationPreferences(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(prefs, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PatchMapping("/settings/notifications")
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> updateNotificationPreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateNotificationPreferencesRequest body,
            HttpServletRequest request) {

        NotificationPreferencesResponse prefs =
                userSettingsService.updateNotificationPreferences(principal.userId(), body);
        return ResponseEntity.ok(ApiResponse.ok(prefs, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @GetMapping("/settings")
    @Operation(summary = "Get account settings")
    public ResponseEntity<ApiResponse<AccountSettingsResponse>> getAccountSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        AccountSettingsResponse settings = userSettingsService.getAccountSettings(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(settings, ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PatchMapping("/settings")
    @Operation(summary = "Update account settings")
    public ResponseEntity<ApiResponse<AccountSettingsResponse>> updateAccountSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateAccountSettingsRequest body,
            HttpServletRequest request) {

        AccountSettingsResponse settings = userSettingsService.updateAccountSettings(principal.userId(), body);
        return ResponseEntity.ok(ApiResponse.ok(settings, ClosiqRequestIdFilter.getRequestId(request)));
    }
}
