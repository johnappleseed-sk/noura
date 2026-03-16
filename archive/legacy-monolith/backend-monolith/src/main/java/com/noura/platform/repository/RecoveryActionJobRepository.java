package com.noura.platform.repository;

import com.noura.platform.domain.entity.RecoveryActionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Repository responsible for bulk destructive-action job persistence.
 */
public interface RecoveryActionJobRepository extends JpaRepository<RecoveryActionJob, UUID>, JpaSpecificationExecutor<RecoveryActionJob> {
}
