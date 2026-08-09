package com.closiq.admin.service;

import com.closiq.admin.web.dto.AdminDashboardResponse;
import com.closiq.catalog.domain.ProductStatus;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.identity.domain.UserStatus;
import com.closiq.identity.repository.UserRepository;
import com.closiq.review.domain.ReviewStatus;
import com.closiq.review.repository.ReviewRepository;
import com.closiq.seller.domain.ApplicationStatus;
import com.closiq.seller.repository.SellerApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final List<ApplicationStatus> PENDING_STATUSES = List.of(
            ApplicationStatus.PENDING,
            ApplicationStatus.UNDER_REVIEW);

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final SellerApplicationRepository sellerApplicationRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        return AdminDashboardResponse.builder()
                .totalUsers(userRepository.countByDeletedAtIsNull())
                .activeUsers(userRepository.countByStatusAndDeletedAtIsNull(UserStatus.ACTIVE))
                .suspendedUsers(userRepository.countByStatusAndDeletedAtIsNull(UserStatus.SUSPENDED))
                .totalProducts(productRepository.countByDeletedAtIsNull())
                .activeProducts(productRepository.countByStatusAndDeletedAtIsNull(ProductStatus.ACTIVE))
                .totalReviews(reviewRepository.count())
                .publishedReviews(reviewRepository.countByStatus(ReviewStatus.PUBLISHED))
                .pendingSellerApplications(sellerApplicationRepository.countByStatusIn(PENDING_STATUSES))
                .build();
    }
}
