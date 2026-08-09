package com.closiq.seller.repository;

import com.closiq.seller.domain.ApplicationStatus;
import com.closiq.seller.domain.SellerApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, UUID> {

    @Query("""
            SELECT sa FROM SellerApplication sa
            WHERE sa.user.id = :userId
            ORDER BY sa.submittedAt DESC
            """)
    List<SellerApplication> findByUserIdOrderBySubmittedAtDesc(@Param("userId") UUID userId);

    Optional<SellerApplication> findFirstByUserIdOrderBySubmittedAtDesc(UUID userId);

    boolean existsByUserIdAndStatusIn(UUID userId, List<ApplicationStatus> statuses);

    @Query("""
            SELECT sa FROM SellerApplication sa
            JOIN FETCH sa.user
            WHERE (:status IS NULL OR sa.status = :status)
            ORDER BY sa.submittedAt DESC
            """)
    List<SellerApplication> findAdminQueue(@Param("status") ApplicationStatus status);

    long countByStatusIn(List<ApplicationStatus> statuses);
}
