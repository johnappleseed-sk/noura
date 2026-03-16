package com.noura.platform.repository;

import com.noura.platform.domain.entity.RecoveryVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsible for immutable recovery snapshots.
 */
public interface RecoveryVersionRepository extends JpaRepository<RecoveryVersion, UUID> {

    /**
     * Lists versions for a governed entity in descending version order.
     *
     * @param tenantKey The tenant key used to scope the lookup.
     * @param entityType The business entity type.
     * @param entityId The business entity identifier.
     * @return Matching versions.
     */
    List<RecoveryVersion> findAllByTenantKeyAndEntityTypeAndEntityIdOrderByVersionNumberDesc(String tenantKey, String entityType, String entityId);

    /**
     * Lists versions for a governed entity in ascending version order.
     *
     * @param tenantKey The tenant key used to scope the lookup.
     * @param entityType The business entity type.
     * @param entityId The business entity identifier.
     * @return Matching versions.
     */
    List<RecoveryVersion> findAllByTenantKeyAndEntityTypeAndEntityIdOrderByVersionNumberAsc(String tenantKey, String entityType, String entityId);

    /**
     * Finds the latest version whose capture time is at or before the requested restore timestamp.
     *
     * @param tenantKey The tenant key used to scope the lookup.
     * @param entityType The business entity type.
     * @param entityId The business entity identifier.
     * @param capturedAt The restore cutoff timestamp.
     * @return The latest matching version when present.
     */
    Optional<RecoveryVersion> findFirstByTenantKeyAndEntityTypeAndEntityIdAndCapturedAtLessThanEqualOrderByCapturedAtDesc(
            String tenantKey,
            String entityType,
            String entityId,
            Instant capturedAt
    );
}
