package com.noura.platform.repository;

import com.noura.platform.domain.entity.ProductApprovalDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductApprovalDecisionRepository extends JpaRepository<ProductApprovalDecision, UUID> {
    List<ProductApprovalDecision> findBySubmissionIdOrderByDecidedAtDesc(UUID submissionId);

    Optional<ProductApprovalDecision> findTopBySubmissionIdOrderByDecidedAtDesc(UUID submissionId);
}
