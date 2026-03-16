package com.noura.platform.service.recovery;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Provides entity-specific persistence behavior for the reusable recovery governance layer.
 */
public interface RecoverableEntityAdapter {

    /**
     * Returns the normalized business entity type supported by the adapter.
     *
     * @return The normalized entity type.
     */
    String getEntityType();

    /**
     * Resolves an existing governed entity handle.
     *
     * @param entityId The business entity identifier.
     * @return The matching handle when present.
     */
    Optional<RecoverableEntityHandle> findHandle(String entityId);

    /**
     * Recreates or restores a governed entity handle from a serialized snapshot.
     *
     * @param entityId The business entity identifier.
     * @param snapshot The serialized snapshot payload.
     * @return The restored entity handle.
     */
    RecoverableEntityHandle restoreHandle(String entityId, JsonNode snapshot);

    /**
     * Permanently removes the governed entity from primary storage.
     *
     * @param entityId The business entity identifier.
     */
    void hardDelete(String entityId);
}
