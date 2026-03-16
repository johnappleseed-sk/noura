package com.noura.platform.repository;

import com.noura.platform.domain.entity.ProductDedupeCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductDedupeCandidateRepository extends JpaRepository<ProductDedupeCandidate, UUID> {
    List<ProductDedupeCandidate> findBySubmissionIdOrderByMatchScoreDesc(UUID submissionId);

    void deleteBySubmissionId(UUID submissionId);
}

