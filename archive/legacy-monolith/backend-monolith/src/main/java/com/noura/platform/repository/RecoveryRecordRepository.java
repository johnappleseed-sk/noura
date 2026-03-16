package com.noura.platform.repository;

import com.noura.platform.domain.entity.RecoveryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsible for persisted lifecycle state records.
 */
public interface RecoveryRecordRepository extends JpaRepository<RecoveryRecord, UUID>, JpaSpecificationExecutor<RecoveryRecord> {

    /**
     * Finds a recovery record by tenant scope and business entity key.
     *
     * @param tenantKey The tenant key used to scope the lookup.
     * @param entityType The business entity type.
     * @param entityId The business entity identifier.
     * @return The matching recovery record when present.
     */
    Optional<RecoveryRecord> findByTenantKeyAndEntityTypeAndEntityId(String tenantKey, String entityType, String entityId);

    /**
     * Finds recovery records for a scoped entity collection.
     *
     * @param tenantKey The tenant key used to scope the lookup.
     * @param entityType The business entity type.
     * @param entityIds The business entity identifiers.
     * @return Matching recovery records.
     */
    List<RecoveryRecord> findAllByTenantKeyAndEntityTypeAndEntityIdIn(String tenantKey, String entityType, Collection<String> entityIds);

    /**
     * Detects whether a governed record already exists under a different tenant.
     *
     * @param entityType The business entity type.
     * @param entityId The business entity identifier.
     * @param tenantKey The tenant key that must be excluded from the check.
     * @return {@code true} when another tenant already governs the same entity id.
     */
    boolean existsByEntityTypeAndEntityIdAndTenantKeyNot(String entityType, String entityId, String tenantKey);
}
