package com.closiq.seller.service;

import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.security.RoleType;
import com.closiq.identity.domain.User;
import com.closiq.identity.service.UserService;
import com.closiq.seller.domain.ApplicationStatus;
import com.closiq.seller.domain.BusinessType;
import com.closiq.seller.domain.SellerApplication;
import com.closiq.seller.repository.KycDocumentRepository;
import com.closiq.seller.repository.MediaAssetRepository;
import com.closiq.seller.repository.SellerApplicationRepository;
import com.closiq.seller.web.dto.SubmitSellerApplicationRequest;
import com.closiq.storage.FileStorageService;
import com.closiq.storage.MediaAssetFactory;
import com.closiq.storage.MediaUploadMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerApplicationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private SellerApplicationRepository applicationRepository;
    @Mock private KycDocumentRepository kycDocumentRepository;
    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private UserService userService;
    @Mock private SellerContextService sellerContextService;
    @Mock private FileStorageService fileStorageService;
    @Mock private MediaAssetFactory mediaAssetFactory;
    @Mock private MediaUploadMapper mediaUploadMapper;

    @InjectMocks
    private SellerApplicationService sellerApplicationService;

    @Test
    void getMyApplication_exposesRejectionReasonAndCanReapplyForRejectedApplication() {
        SellerApplication application = rejectedApplication("Incomplete business registration document");
        when(applicationRepository.findFirstByUserIdOrderBySubmittedAtDesc(USER_ID))
                .thenReturn(Optional.of(application));
        when(kycDocumentRepository.findByApplicationIdOrderByUploadedAtAsc(application.getId()))
                .thenReturn(List.of());
        when(userService.getUserRoles(USER_ID)).thenReturn(List.of(RoleType.CUSTOMER));

        var response = sellerApplicationService.getMyApplication(USER_ID);

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getRejectionReason()).isEqualTo("Incomplete business registration document");
        assertThat(response.isCanReapply()).isTrue();
    }

    @Test
    void getMyApplication_doesNotAllowReapplyWhenUserAlreadyHasSellerRole() {
        SellerApplication application = rejectedApplication("Missing PAN copy");
        when(applicationRepository.findFirstByUserIdOrderBySubmittedAtDesc(USER_ID))
                .thenReturn(Optional.of(application));
        when(kycDocumentRepository.findByApplicationIdOrderByUploadedAtAsc(application.getId()))
                .thenReturn(List.of());
        when(userService.getUserRoles(USER_ID)).thenReturn(List.of(RoleType.CUSTOMER, RoleType.SELLER));

        var response = sellerApplicationService.getMyApplication(USER_ID);

        assertThat(response.isCanReapply()).isFalse();
    }

    @Test
    void submitApplication_allowsNewApplicationAfterRejection() {
        SubmitSellerApplicationRequest request = new SubmitSellerApplicationRequest(
                "House of Meera", "INDIVIDUAL", "Mumbai", null, null, "ABCDE1234F");

        when(applicationRepository.existsByUserIdAndStatusIn(eq(USER_ID), any())).thenReturn(false);
        when(userService.requireActiveUser(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = sellerApplicationService.submitApplication(USER_ID, request);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(sellerContextService).ensureNotSeller(USER_ID);
        verify(applicationRepository).save(any(SellerApplication.class));
    }

    @Test
    void submitApplication_blocksWhenActiveApplicationExists() {
        when(applicationRepository.existsByUserIdAndStatusIn(eq(USER_ID), any())).thenReturn(true);

        SubmitSellerApplicationRequest request = new SubmitSellerApplicationRequest(
                "House of Meera", "INDIVIDUAL", "Mumbai", null, null, "ABCDE1234F");

        assertThatThrownBy(() -> sellerApplicationService.submitApplication(USER_ID, request))
                .isInstanceOf(ClosiqException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_EXISTS);
    }

    private SellerApplication rejectedApplication(String reason) {
        return SellerApplication.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(USER_ID).build())
                .businessName("House of Meera")
                .businessType(BusinessType.INDIVIDUAL)
                .city("Mumbai")
                .panNumber("ABCDE1234F")
                .status(ApplicationStatus.REJECTED)
                .rejectionReason(reason)
                .submittedAt(Instant.parse("2026-08-01T10:00:00Z"))
                .reviewedAt(Instant.parse("2026-08-02T10:00:00Z"))
                .build();
    }
}
