package com.closiq.user.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.identity.repository.UserRepository;
import com.closiq.identity.service.UserService;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.mapper.UserProfileMapper;
import com.closiq.user.repository.SellerProfileRepository;
import com.closiq.user.web.dto.UpdateProfileRequest;
import com.closiq.user.web.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final UserService userService;
    private final UserProfileMapper userProfileMapper;
    private final UserPreferencesHelper preferencesHelper;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userService.requireActiveUser(userId);
        UserProfile profile = userService.requireProfile(userId);
        var roles = userService.getUserRoles(userId);
        SellerProfile sellerProfile = sellerProfileRepository.findByUserId(userId).orElse(null);
        return userProfileMapper.toProfileResponse(user, profile, roles, sellerProfile);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userService.requireActiveUser(userId);
        UserProfile profile = userService.requireProfile(userId);

        if (request.getFirstName() != null) {
            profile.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            profile.setLastName(request.getLastName());
        }
        if (request.getFirstName() != null || request.getLastName() != null) {
            profile.setDisplayName(UserService.buildDisplayName(profile.getFirstName(), profile.getLastName()));
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }

        if (request.getEmail() != null) {
            userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.getEmail())
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing -> {
                        throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "Email is already in use");
                    });
            user.setEmail(request.getEmail());
            user.setEmailVerified(false);
        }

        if (request.getAlternatePhone() != null) {
            String alternatePhone = request.getAlternatePhone().isBlank() ? null : request.getAlternatePhone();
            if (alternatePhone != null && alternatePhone.equals(user.getPhone())) {
                throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Alternate phone must differ from primary phone");
            }
            user.setAlternatePhone(alternatePhone);
        }

        if (request.getAlternateEmail() != null) {
            String alternateEmail = request.getAlternateEmail().isBlank() ? null : request.getAlternateEmail();
            if (alternateEmail != null) {
                userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(alternateEmail)
                        .filter(existing -> !existing.getId().equals(userId))
                        .ifPresent(existing -> {
                            throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "Alternate email is already in use");
                        });
                if (alternateEmail.equalsIgnoreCase(user.getEmail())) {
                    throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Alternate email must differ from primary email");
                }
            }
            user.setAlternateEmail(alternateEmail);
        }

        Map<String, Object> preferences = preferencesHelper.read(profile.getPreferences());

        if (request.getAvatarUrl() != null) {
            preferences = preferencesHelper.withAvatarUrl(preferences, request.getAvatarUrl());
        }

        if (request.getPreferences() != null) {
            preferences = preferencesHelper.mergeShopping(
                    preferences,
                    new UserPreferencesHelper.ShoppingPreferences(
                            request.getPreferences().getSize(),
                            request.getPreferences().getOccasions()));
        }

        profile.setPreferences(preferences);
        userRepository.save(user);
        userProfileRepository.save(profile);

        var roles = userService.getUserRoles(userId);
        SellerProfile sellerProfile = sellerProfileRepository.findByUserId(userId).orElse(null);
        return userProfileMapper.toProfileResponse(user, profile, roles, sellerProfile);
    }
}
