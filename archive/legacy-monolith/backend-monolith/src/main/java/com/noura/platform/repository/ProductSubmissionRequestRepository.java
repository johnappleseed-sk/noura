package com.noura.platform.repository;

import com.noura.platform.domain.entity.ProductSubmissionRequest;
import com.noura.platform.domain.enums.ProductSubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProductSubmissionRequestRepository extends JpaRepository<ProductSubmissionRequest, UUID>, JpaSpecificationExecutor<ProductSubmissionRequest> {
    Optional<ProductSubmissionRequest> findByIdAndStoreId(UUID id, UUID storeId);

    long countByStatus(ProductSubmissionStatus status);
}

