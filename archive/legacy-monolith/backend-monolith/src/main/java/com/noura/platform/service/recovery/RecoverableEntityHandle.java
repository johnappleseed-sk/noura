package com.noura.platform.service.recovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.noura.platform.domain.enums.RecoveryLifecycleState;

/**
 * Defines a mutable, persistable handle over a governed business entity instance.
 */
public interface RecoverableEntityHandle {

    /**
     * Returns the business entity identifier.
     *
     * @return The business entity identifier.
     */
    String getEntityId();

    /**
     * Returns an operator-facing display name for the entity.
     *
     * @return The display name.
     */
    String getDisplayName();

    /**
     * Produces a serializable snapshot of the current entity state.
     *
     * @return The current entity snapshot.
     */
    Object toSnapshot();

    /**
     * Applies the requested lifecycle state to the current entity instance.
     *
     * @param state The lifecycle state to apply.
     */
    void applyLifecycleState(RecoveryLifecycleState state);

    /**
     * Restores the current entity instance from a serialized snapshot.
     *
     * @param snapshot The serialized snapshot payload.
     * @param targetState The lifecycle state to apply after restoration.
     */
    void restoreFromSnapshot(JsonNode snapshot, RecoveryLifecycleState targetState);

    /**
     * Persists the current entity instance.
     */
    void persist();
}
