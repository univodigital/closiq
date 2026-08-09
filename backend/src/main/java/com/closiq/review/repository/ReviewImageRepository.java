package com.closiq.review.repository;

import com.closiq.review.domain.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, UUID> {

    List<ReviewImage> findByReviewIdOrderBySortOrderAsc(UUID reviewId);
}
