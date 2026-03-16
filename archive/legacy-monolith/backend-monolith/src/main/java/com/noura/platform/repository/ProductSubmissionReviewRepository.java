package com.noura.platform.repository;

import com.noura.platform.domain.entity.ProductSubmissionReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductSubmissionReviewRepository extends JpaRepository<ProductSubmissionReview, UUID> {
    List<ProductSubmissionReview> findBySubmissionIdOrderByOccurredAtDesc(UUID submissionId);
}

