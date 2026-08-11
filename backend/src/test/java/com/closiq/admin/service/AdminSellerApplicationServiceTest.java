package com.closiq.admin.service;

import com.closiq.admin.web.dto.RejectSellerApplicationRequest;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.exception.ErrorCode;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.service.UserService;
import com.closiq.seller.domain.ApplicationStatus;
import com.closiq.seller.domain.BusinessType;
import com.closiq.seller.domain.SellerApplication;
import com.closiq.seller.repository.SellerApplicationRepository;
import com.closiq.seller.repository.WalletRepository;
import com.closiq.user.repository.SellerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSellerApplicationServiceTest {

    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private SellerApplicationRepository applicationRepository;
    @Mock private SellerProfileRepository sellerProfileRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private UserService userService;

    @InjectMocks
    private AdminSellerApplicationService adminSellerApplicationService;

    @Test
    void rejectApplication_persistsTrimmedRejectionReason() {
        SellerApplication application = pendingApplication();
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userService.requireProfile(USER_ID)).thenReturn(UserProfile.builder().displayName("Meera").build());

        var response = adminSellerApplicationService.rejectApplication(
                APPLICATION_ID,
                new RejectSellerApplicationRequest("  Incomplete business registration document  "),
                ADMIN_ID);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(application.getRejectionReason()).isEqualTo("Incomplete business registration document");
        assertThat(application.getReviewedAt()).isNotNull();
        assertThat(application.getUpdatedBy()).isEqualTo(ADMIN_ID);
        assertThat(response.getRejectionReason()).isEqualTo("Incomplete business registration document");
        assertThat(response.getStatus()).isEqualTo("REJECTED");
    }

    @Test
    void rejectApplication_rejectsNonReviewableApplication() {
        SellerApplication application = pendingApplication();
        application.setStatus(ApplicationStatus.REJECTED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> adminSellerApplicationService.rejectApplication(
                        APPLICATION_ID,
                        new RejectSellerApplicationRequest("Already rejected reason text"),
                        ADMIN_ID))
                .isInstanceOf(ClosiqException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATE_TRANSITION);

        verify(applicationRepository, never()).save(any());
    }

    private SellerApplication pendingApplication() {
        return SellerApplication.builder()
                .id(APPLICATION_ID)
                .user(User.builder().id(USER_ID).phone("+919876543210").build())
                .businessName("House of Meera")
                .businessType(BusinessType.INDIVIDUAL)
                .city("Mumbai")
                .panNumber("ABCDE1234F")
                .status(ApplicationStatus.PENDING)
                .submittedAt(Instant.parse("2026-08-01T10:00:00Z"))
                .build();
    }
}
