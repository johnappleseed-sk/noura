package com.noura.platform.service.recovery;

import com.noura.platform.common.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves recoverable entity adapters by normalized business entity type.
 */
@Service
public class RecoverableEntityRegistry {

    private final Map<String, RecoverableEntityAdapter> adaptersByType;

    /**
     * Creates a new adapter registry.
     *
     * @param adapters The registered recoverable entity adapters.
     */
    public RecoverableEntityRegistry(List<RecoverableEntityAdapter> adapters) {
        this.adaptersByType = adapters.stream()
                .collect(Collectors.toUnmodifiableMap(
                        adapter -> normalize(adapter.getEntityType()),
                        Function.identity()
                ));
    }

    /**
     * Resolves a required adapter by business entity type.
     *
     * @param entityType The business entity type.
     * @return The matching adapter.
     */
    public RecoverableEntityAdapter getRequiredAdapter(String entityType) {
        RecoverableEntityAdapter adapter = adaptersByType.get(normalize(entityType));
        if (adapter == null) {
            throw new BadRequestException("RECOVERY_ENTITY_TYPE_UNSUPPORTED", "Unsupported recovery entity type: " + entityType);
        }
        return adapter;
    }

    /**
     * Normalizes entity-type keys for registry lookups.
     *
     * @param entityType The raw entity type.
     * @return The normalized entity type.
     */
    public String normalize(String entityType) {
        return entityType == null ? null : entityType.trim().toUpperCase(Locale.ROOT);
    }
}
