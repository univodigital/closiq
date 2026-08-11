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
import com.closiq.identity.service.RefreshTokenService;
import com.closiq.seller.web.dto.PresignedUploadResponse;
import com.closiq.user.service.AccountSecurityService;
import com.closiq.user.service.UserAvatarService;
import com.closiq.identity.web.dto.OtpInitiateResponse;
import com.closiq.user.web.dto.AvatarUploadUrlRequest;
import com.closiq.user.web.dto.ChangePasswordRequest;
import com.closiq.user.web.dto.ChangeUsernameRequest;
import com.closiq.user.web.dto.ConfirmAvatarRequest;
import com.closiq.user.web.dto.DeleteAccountPreviewResponse;
import com.closiq.user.web.dto.NewPhoneOtpRequest;
import com.closiq.user.web.dto.RequestEmailChangeRequest;
import com.closiq.user.web.dto.VerifyPhoneChangeOtpRequest;
import jakarta.servlet.http.Cookie;
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
import java.util.Map;
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
    private final AccountSecurityService accountSecurityService;
    private final UserAvatarService userAvatarService;

    @GetMapping("/delete-preview")
    @Operation(summary = "Preview account deletion impact")
    public ResponseEntity<ApiResponse<DeleteAccountPreviewResponse>> deletePreview(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                userAccountService.previewDeleteAccount(principal.userId()),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/phone-change/initiate")
    @Operation(summary = "Send OTP to current phone to start phone change")
    public ResponseEntity<ApiResponse<OtpInitiateResponse>> initiatePhoneChange(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                accountSecurityService.initiatePhoneChange(principal.userId()),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/phone-change/verify-old")
    @Operation(summary = "Verify OTP for current phone")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyOldPhone(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VerifyPhoneChangeOtpRequest body,
            HttpServletRequest request) {
        accountSecurityService.verifyOldPhone(principal.userId(), body.getOtpSessionId(), body.getOtp());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("verified", true), ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/phone-change/send-new")
    @Operation(summary = "Send OTP to new phone number")
    public ResponseEntity<ApiResponse<OtpInitiateResponse>> sendNewPhoneOtp(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NewPhoneOtpRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                accountSecurityService.sendNewPhoneOtp(principal.userId(), body.getNewPhone()),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/phone-change/complete")
    @Operation(summary = "Verify new phone OTP and update phone number")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> completePhoneChange(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VerifyPhoneChangeOtpRequest body,
            HttpServletRequest request) {
        accountSecurityService.completePhoneChange(principal.userId(), body.getOtpSessionId(), body.getOtp());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("updated", true), ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/email-change/request")
    @Operation(summary = "Request email change and send verification OTP")
    public ResponseEntity<ApiResponse<OtpInitiateResponse>> requestEmailChange(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RequestEmailChangeRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                accountSecurityService.requestEmailChange(principal.userId(), body.getNewEmail()),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/email-change/verify")
    @Operation(summary = "Verify email change OTP")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyEmailChange(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VerifyPhoneChangeOtpRequest body,
            HttpServletRequest request) {
        accountSecurityService.verifyEmailChange(principal.userId(), body.getOtpSessionId(), body.getOtp());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("verified", true), ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/password")
    @Operation(summary = "Change password and sign out other devices")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest body,
            HttpServletRequest httpRequest) {
        if (!body.getNewPassword().equals(body.getConfirmPassword())) {
            throw new com.closiq.common.exception.ClosiqException(
                    com.closiq.common.exception.ErrorCode.VALIDATION_ERROR, "Passwords do not match");
        }
        accountSecurityService.changePassword(
                principal.userId(),
                body.getCurrentPassword(),
                body.getNewPassword(),
                extractRefreshToken(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok(Map.of("updated", true), ClosiqRequestIdFilter.getRequestId(httpRequest)));
    }

    @PostMapping("/username")
    @Operation(summary = "Change username once")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> changeUsername(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangeUsernameRequest body,
            HttpServletRequest request) {
        accountSecurityService.changeUsername(principal.userId(), body.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("updated", true), ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/avatar/upload-url")
    @Operation(summary = "Create presigned avatar upload URL")
    public ResponseEntity<ApiResponse<PresignedUploadResponse>> avatarUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AvatarUploadUrlRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                userAvatarService.createUploadUrl(
                        principal.userId(), body.getFileName(), body.getContentType(), body.getFileSize()),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/avatar/confirm")
    @Operation(summary = "Confirm avatar upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> confirmAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ConfirmAvatarRequest body,
            HttpServletRequest request) {
        String url = userAvatarService.confirmAvatar(
                principal.userId(), UUID.fromString(body.getUploadId()));
        return ResponseEntity.ok(ApiResponse.ok(Map.of("avatarUrl", url), ClosiqRequestIdFilter.getRequestId(request)));
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Remove avatar")
    public ResponseEntity<Void> removeAvatar(@AuthenticationPrincipal UserPrincipal principal) {
        userAvatarService.removeAvatar(principal.userId());
        return ResponseEntity.noContent().build();
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (RefreshTokenService.REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

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
