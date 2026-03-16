package com.noura.platform.service;

import com.noura.platform.domain.enums.ProductSubmissionStatus;
import com.noura.platform.dto.submission.ProductSubmissionCreateRequest;
import com.noura.platform.dto.submission.ProductSubmissionDecisionRequest;
import com.noura.platform.dto.submission.ProductSubmissionDetailDto;
import com.noura.platform.dto.submission.ProductSubmissionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Store product onboarding workflow: submit -> review -> approve/reject/link duplicates.
 */
public interface ProductSubmissionService {
    ProductSubmissionDto submit(UUID storeId, ProductSubmissionCreateRequest request);

    ProductSubmissionDto resubmit(UUID storeId, UUID submissionId, ProductSubmissionCreateRequest request);

    Page<ProductSubmissionDto> listForStore(UUID storeId, ProductSubmissionStatus status, Pageable pageable);

    Page<ProductSubmissionDto> listForAdmin(ProductSubmissionStatus status, String query, Boolean duplicatesOnly, Pageable pageable);

    ProductSubmissionDetailDto getForStore(UUID storeId, UUID submissionId);

    ProductSubmissionDetailDto getForAdmin(UUID submissionId);

    ProductSubmissionDto approve(UUID submissionId, ProductSubmissionDecisionRequest request);

    ProductSubmissionDto reject(UUID submissionId, ProductSubmissionDecisionRequest request);

    ProductSubmissionDto requestRevision(UUID submissionId, ProductSubmissionDecisionRequest request);

    ProductSubmissionDto linkDuplicate(UUID submissionId, ProductSubmissionDecisionRequest request);
}

