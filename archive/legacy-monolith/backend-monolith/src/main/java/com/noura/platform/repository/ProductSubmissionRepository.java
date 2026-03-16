package com.noura.platform.repository;

import com.noura.platform.domain.entity.ProductSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ProductSubmissionRepository extends JpaRepository<ProductSubmission, UUID>, JpaSpecificationExecutor<ProductSubmission> {
}
