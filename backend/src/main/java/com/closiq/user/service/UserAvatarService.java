package com.closiq.user.service;

import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.util.IdGenerator;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.identity.service.UserService;
import com.closiq.seller.domain.MediaAsset;
import com.closiq.seller.repository.MediaAssetRepository;
import com.closiq.seller.web.dto.PresignedUploadResponse;
import com.closiq.storage.FileStorageService;
import com.closiq.storage.MediaAssetFactory;
import com.closiq.storage.MediaUploadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAvatarService {

    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp");

    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaAssetFactory mediaAssetFactory;
    private final FileStorageService fileStorageService;
    private final MediaUploadMapper mediaUploadMapper;
    private final UserPreferencesHelper preferencesHelper;

    @Transactional
    public PresignedUploadResponse createUploadUrl(UUID userId, String fileName, String contentType, long fileSize) {
        validateUpload(contentType, fileSize);
        User user = userService.requireActiveUser(userId);

        UUID uploadId = IdGenerator.uuidV7();
        String relativePath = "users/" + userId + "/avatar/" + uploadId;

        MediaAsset asset = mediaAssetFactory.createPendingUpload(
                uploadId, user, relativePath, fileName, contentType);
        mediaAssetRepository.save(asset);

        var instruction = fileStorageService.createUploadInstruction(relativePath, contentType);
        return mediaUploadMapper.toPresignedResponse(uploadId, instruction);
    }

    @Transactional
    public String confirmAvatar(UUID userId, UUID uploadId) {
        userService.requireActiveUser(userId);
        UserProfile profile = userService.requireProfile(userId);

        MediaAsset asset = mediaAssetRepository.findByIdAndUploadedById(uploadId, userId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Upload not found"));

        if (!ALLOWED_TYPES.contains(asset.getMimeType().toLowerCase())) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Unsupported image type");
        }

        asset.setStatus("ATTACHED");
        mediaAssetRepository.save(asset);

        String publicUrl = fileStorageService.resolvePublicUrl(asset);
        profile.setAvatarMediaId(asset.getId());
        profile.setPreferences(preferencesHelper.withAvatarUrl(
                preferencesHelper.read(profile.getPreferences()), publicUrl));
        userProfileRepository.save(profile);
        return publicUrl;
    }

    @Transactional
    public void removeAvatar(UUID userId) {
        UserProfile profile = userService.requireProfile(userId);
        profile.setAvatarMediaId(null);
        profile.setPreferences(preferencesHelper.withAvatarUrl(
                preferencesHelper.read(profile.getPreferences()), null));
        userProfileRepository.save(profile);
    }

    private void validateUpload(String contentType, long fileSize) {
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Only JPEG, PNG, or WebP images are allowed");
        }
        if (fileSize <= 0 || fileSize > MAX_BYTES) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Image must be 5 MB or smaller");
        }
    }
}
