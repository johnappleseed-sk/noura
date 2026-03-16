package com.noura.platform.repository;

import com.noura.platform.domain.entity.RecoveryAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Repository responsible for recovery audit log persistence.
 */
public interface RecoveryAuditLogRepository extends JpaRepository<RecoveryAuditLog, UUID>, JpaSpecificationExecutor<RecoveryAuditLog> {
}
