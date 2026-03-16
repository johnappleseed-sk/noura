package com.noura.platform.service;

import com.noura.platform.domain.enums.SubmissionStatus;
import com.noura.platform.dto.superinventory.ApproveProductSubmissionRequest;
import com.noura.platform.dto.superinventory.CreateProductSubmissionRequest;
import com.noura.platform.dto.superinventory.ProductSubmissionDetailResponse;
import com.noura.platform.dto.superinventory.ProductSubmissionResponse;
import com.noura.platform.dto.superinventory.RejectProductSubmissionRequest;
import com.noura.platform.dto.superinventory.StoreProductReferenceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SuperInventoryService {
    ProductSubmissionResponse submitProductCandidate(CreateProductSubmissionRequest request);

    Page<ProductSubmissionResponse> listProductSubmissions(
            SubmissionStatus status,
            UUID merchantId,
            UUID storeId,
            String query,
            Pageable pageable
    );

    ProductSubmissionDetailResponse getProductSubmission(UUID submissionId);

    ProductSubmissionResponse approveProductSubmission(UUID submissionId, ApproveProductSubmissionRequest request);

    ProductSubmissionResponse rejectProductSubmission(UUID submissionId, RejectProductSubmissionRequest request);

    Page<StoreProductReferenceResponse> listStoreProductReferences(UUID storeId, UUID productId, Boolean active, Pageable pageable);

    StoreProductReferenceResponse linkStoreProduct(UUID storeId, UUID productId);
}
