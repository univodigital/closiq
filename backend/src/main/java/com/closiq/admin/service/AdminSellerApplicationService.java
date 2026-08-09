package com.closiq.admin.service;

import com.closiq.admin.web.dto.AdminSellerApplicationListItemResponse;
import com.closiq.admin.web.dto.RejectSellerApplicationRequest;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.security.RoleType;
import com.closiq.common.util.IdGenerator;
import com.closiq.identity.domain.User;
import com.closiq.identity.domain.UserProfile;
import com.closiq.identity.service.UserService;
import com.closiq.seller.domain.ApplicationStatus;
import com.closiq.seller.domain.SellerApplication;
import com.closiq.seller.domain.Wallet;
import com.closiq.seller.repository.SellerApplicationRepository;
import com.closiq.seller.repository.WalletRepository;
import com.closiq.user.domain.SellerProfile;
import com.closiq.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminSellerApplicationService {

    private static final EnumSet<ApplicationStatus> REVIEWABLE = EnumSet.of(
            ApplicationStatus.PENDING,
            ApplicationStatus.UNDER_REVIEW);

    private final SellerApplicationRepository applicationRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final WalletRepository walletRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<AdminSellerApplicationListItemResponse> listApplications(String status) {
        ApplicationStatus filter = status != null && !status.isBlank()
                ? ApplicationStatus.valueOf(status)
                : null;

        return applicationRepository.findAdminQueue(filter).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional
    public AdminSellerApplicationListItemResponse approveApplication(UUID applicationId, UUID adminId) {
        SellerApplication application = requireReviewableApplication(applicationId);

        if (sellerProfileRepository.findByUserId(application.getUser().getId()).isPresent()) {
            throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "User already has a seller profile");
        }

        Instant now = Instant.now();
        application.setStatus(ApplicationStatus.VERIFIED);
        application.setReviewedAt(now);
        application.setUpdatedBy(adminId);
        applicationRepository.save(application);

        SellerProfile sellerProfile = SellerProfile.builder()
                .id(IdGenerator.uuidV7())
                .user(application.getUser())
                .businessName(application.getBusinessName())
                .status("ACTIVE")
                .city(application.getCity())
                .createdAt(now)
                .updatedAt(now)
                .build();
        sellerProfileRepository.save(sellerProfile);

        userService.assignRole(application.getUser(), RoleType.SELLER);

        walletRepository.save(Wallet.builder()
                .id(IdGenerator.uuidV7())
                .sellerProfile(sellerProfile)
                .availableBalance(0)
                .pendingBalance(0)
                .totalEarned(0)
                .totalWithdrawn(0)
                .currencyCode("INR")
                .build());

        return toListItem(application);
    }

    @Transactional
    public AdminSellerApplicationListItemResponse rejectApplication(
            UUID applicationId,
            RejectSellerApplicationRequest request,
            UUID adminId) {

        SellerApplication application = requireReviewableApplication(applicationId);

        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionReason(request.getReason());
        application.setReviewedAt(Instant.now());
        application.setUpdatedBy(adminId);
        applicationRepository.save(application);

        return toListItem(application);
    }

    private SellerApplication requireReviewableApplication(UUID applicationId) {
        SellerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Seller application not found"));

        if (!REVIEWABLE.contains(application.getStatus())) {
            throw new ClosiqException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    "Application is not pending review");
        }
        return application;
    }

    private AdminSellerApplicationListItemResponse toListItem(SellerApplication application) {
        User user = application.getUser();
        UserProfile profile = userService.requireProfile(user.getId());

        return AdminSellerApplicationListItemResponse.builder()
                .applicationId(application.getId().toString())
                .userId(user.getId().toString())
                .applicantName(profile.getDisplayName())
                .applicantPhone(user.getPhone())
                .businessName(application.getBusinessName())
                .businessType(application.getBusinessType().name())
                .city(application.getCity())
                .status(application.getStatus().name())
                .submittedAt(application.getSubmittedAt())
                .build();
    }
}
