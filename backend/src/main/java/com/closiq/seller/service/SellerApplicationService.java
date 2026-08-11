package com.closiq.seller.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.IdGenerator;
import com.closiq.storage.FileStorageService;
import com.closiq.storage.MediaAssetFactory;
import com.closiq.storage.MediaUploadMapper;
import com.closiq.common.security.RoleType;
import com.closiq.identity.domain.User;
import com.closiq.identity.service.UserService;
import com.closiq.seller.domain.ApplicationStatus;
import com.closiq.seller.domain.BusinessType;
import com.closiq.seller.domain.KycDocument;
import com.closiq.seller.domain.KycDocumentStatus;
import com.closiq.seller.domain.KycDocumentType;
import com.closiq.seller.domain.MediaAsset;
import com.closiq.seller.domain.SellerApplication;
import com.closiq.seller.repository.KycDocumentRepository;
import com.closiq.seller.repository.MediaAssetRepository;
import com.closiq.seller.repository.SellerApplicationRepository;
import com.closiq.seller.web.dto.ConfirmKycDocumentRequest;
import com.closiq.seller.web.dto.KycDocumentSummaryResponse;
import com.closiq.seller.web.dto.KycUploadUrlRequest;
import com.closiq.seller.web.dto.PresignedUploadResponse;
import com.closiq.seller.web.dto.SellerApplicationDetailResponse;
import com.closiq.seller.web.dto.SellerApplicationSubmitResponse;
import com.closiq.seller.web.dto.SubmitSellerApplicationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerApplicationService {

    private static final List<ApplicationStatus> BLOCKING_STATUSES = List.of(
            ApplicationStatus.PENDING,
            ApplicationStatus.UNDER_REVIEW,
            ApplicationStatus.VERIFIED);

    private final SellerApplicationRepository applicationRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final UserService userService;
    private final SellerContextService sellerContextService;
    private final FileStorageService fileStorageService;
    private final MediaAssetFactory mediaAssetFactory;
    private final MediaUploadMapper mediaUploadMapper;

    @Transactional
    public SellerApplicationSubmitResponse submitApplication(UUID userId, SubmitSellerApplicationRequest request) {
        sellerContextService.ensureNotSeller(userId);

        if (applicationRepository.existsByUserIdAndStatusIn(userId, BLOCKING_STATUSES)) {
            throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "An active seller application already exists");
        }

        User user = userService.requireActiveUser(userId);
        SellerApplication application = SellerApplication.builder()
                .id(IdGenerator.uuidV7())
                .user(user)
                .businessName(request.getBusinessName())
                .businessType(BusinessType.valueOf(request.getBusinessType()))
                .city(request.getCity())
                .description(request.getDescription())
                .gstNumber(blankToNull(request.getGstNumber()))
                .panNumber(request.getPanNumber().toUpperCase())
                .status(ApplicationStatus.PENDING)
                .submittedAt(Instant.now())
                .build();

        applicationRepository.save(application);

        return SellerApplicationSubmitResponse.builder()
                .applicationId(application.getId().toString())
                .status(application.getStatus().name())
                .submittedAt(application.getSubmittedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public SellerApplicationDetailResponse getMyApplication(UUID userId) {
        SellerApplication application = applicationRepository.findFirstByUserIdOrderBySubmittedAtDesc(userId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "No seller application found"));

        List<KycDocumentSummaryResponse> documents = kycDocumentRepository
                .findByApplicationIdOrderByUploadedAtAsc(application.getId()).stream()
                .map(this::toDocumentSummary)
                .toList();

        return SellerApplicationDetailResponse.builder()
                .applicationId(application.getId().toString())
                .status(mapApplicationStatusForApi(application.getStatus()))
                .businessName(application.getBusinessName())
                .submittedAt(application.getSubmittedAt())
                .reviewedAt(application.getReviewedAt())
                .rejectionReason(application.getRejectionReason())
                .canReapply(canReapply(userId, application))
                .documents(documents)
                .build();
    }

    @Transactional
    public PresignedUploadResponse createKycUploadUrl(UUID userId, KycUploadUrlRequest request) {
        SellerApplication application = requireOpenApplication(userId);
        UUID uploadId = IdGenerator.uuidV7();
        User user = userService.requireActiveUser(userId);

        String relativePath = "kyc/" + userId + "/" + request.getDocumentType().toLowerCase() + "/" + uploadId;

        MediaAsset asset = mediaAssetFactory.createPendingUpload(
                uploadId, user, relativePath, request.getFileName(), request.getContentType());
        mediaAssetRepository.save(asset);

        var instruction = fileStorageService.createUploadInstruction(relativePath, request.getContentType());
        return mediaUploadMapper.toPresignedResponse(uploadId, instruction);
    }

    @Transactional
    public KycDocumentSummaryResponse confirmKycDocument(UUID userId, ConfirmKycDocumentRequest request) {
        SellerApplication application = requireOpenApplication(userId);
        UUID uploadId = UUID.fromString(request.getUploadId());

        MediaAsset asset = mediaAssetRepository.findByIdAndUploadedById(uploadId, userId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Upload not found"));

        asset.setStatus("ATTACHED");
        mediaAssetRepository.save(asset);

        KycDocument document = KycDocument.builder()
                .id(IdGenerator.uuidV7())
                .application(application)
                .documentType(KycDocumentType.valueOf(request.getDocumentType()))
                .mediaAsset(asset)
                .status(KycDocumentStatus.UPLOADED)
                .uploadedAt(Instant.now())
                .build();
        kycDocumentRepository.save(document);

        return toDocumentSummary(document);
    }

    private SellerApplication requireOpenApplication(UUID userId) {
        SellerApplication application = applicationRepository.findFirstByUserIdOrderBySubmittedAtDesc(userId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "No seller application found"));

        if (!EnumSet.of(ApplicationStatus.PENDING, ApplicationStatus.UNDER_REVIEW, ApplicationStatus.DRAFT)
                .contains(application.getStatus())) {
            throw new ClosiqException(ErrorCode.FORBIDDEN, "Application is not open for document uploads");
        }
        return application;
    }

    private KycDocumentSummaryResponse toDocumentSummary(KycDocument document) {
        return KycDocumentSummaryResponse.builder()
                .type(document.getDocumentType().name())
                .status(mapKycStatusForApi(document.getStatus()))
                .uploadedAt(document.getUploadedAt())
                .build();
    }

    private boolean canReapply(UUID userId, SellerApplication application) {
        if (application.getStatus() != ApplicationStatus.REJECTED) {
            return false;
        }
        return !userService.getUserRoles(userId).contains(RoleType.SELLER);
    }

    private String mapApplicationStatusForApi(ApplicationStatus status) {
        return status == ApplicationStatus.VERIFIED ? "VERIFIED" : status.name();
    }

    private String mapKycStatusForApi(KycDocumentStatus status) {
        return switch (status) {
            case APPROVED -> "APPROVED";
            case REJECTED -> "REJECTED";
            case UPLOADED, PENDING_REVIEW -> "UPLOADED";
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
