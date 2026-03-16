package com.noura.platform.repository;

import com.noura.platform.domain.entity.RecoveryActionApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsible for recovery approval request persistence.
 */
public interface RecoveryActionApprovalRepository extends JpaRepository<RecoveryActionApproval, UUID>, JpaSpecificationExecutor<RecoveryActionApproval> {

    Optional<RecoveryActionApproval> findByIdAndTenantKey(UUID id, String tenantKey);
}

