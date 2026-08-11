package com.closiq.user.service;

import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.repository.UserProfileRepository;
import com.closiq.identity.service.UserService;
import com.closiq.seller.domain.MediaAsset;
import com.closiq.seller.repository.MediaAssetRepository;
import com.closiq.storage.FileStorageService;
import com.closiq.storage.MediaAssetFactory;
import com.closiq.storage.MediaUploadMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAvatarServiceTest {

    @Mock private UserService userService;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private MediaAssetFactory mediaAssetFactory;
    @Mock private FileStorageService fileStorageService;
    @Mock private MediaUploadMapper mediaUploadMapper;
    @Mock private UserPreferencesHelper preferencesHelper;

    @InjectMocks
    private UserAvatarService userAvatarService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void createUploadUrl_rejectsInvalidContentType() {
        assertThatThrownBy(() -> userAvatarService.createUploadUrl(userId, "doc.pdf", "application/pdf", 1024))
                .isInstanceOf(ClosiqException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);

        verify(userService, never()).requireActiveUser(userId);
    }

    @Test
    void createUploadUrl_rejectsOversizedFile() {
        assertThatThrownBy(() ->
                        userAvatarService.createUploadUrl(userId, "photo.jpg", "image/jpeg", 6 * 1024 * 1024))
                .isInstanceOf(ClosiqException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void confirmAvatar_rejectsUnsupportedMimeType() {
        UUID uploadId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder().userId(userId).build();
        MediaAsset asset = MediaAsset.builder().id(uploadId).mimeType("application/pdf").build();

        when(userService.requireActiveUser(userId)).thenReturn(User.builder().id(userId).build());
        when(userService.requireProfile(userId)).thenReturn(profile);
        when(mediaAssetRepository.findByIdAndUploadedById(uploadId, userId)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> userAvatarService.confirmAvatar(userId, uploadId))
                .isInstanceOf(ClosiqException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);

        verify(userProfileRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
