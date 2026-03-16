package com.noura.platform.service.impl.recovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.ServiceArea;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.enums.RecoveryLifecycleState;
import com.noura.platform.domain.enums.ServiceAreaStatus;
import com.noura.platform.domain.enums.ServiceAreaType;
import com.noura.platform.repository.ServiceAreaRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.service.recovery.RecoverableEntityAdapter;
import com.noura.platform.service.recovery.RecoverableEntityHandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Implements recoverable-entity behavior for service-area governance.
 */
@Component
public class ServiceAreaRecoveryAdapter implements RecoverableEntityAdapter {
    private static final String ENTITY_TYPE = "SERVICE_AREA";

    private final ServiceAreaRepository serviceAreaRepository;
    private final StoreRepository storeRepository;

    /**
     * Creates a new service-area recovery adapter.
     *
     * @param serviceAreaRepository The service-area repository.
     * @param storeRepository The store repository.
     */
    public ServiceAreaRecoveryAdapter(ServiceAreaRepository serviceAreaRepository, StoreRepository storeRepository) {
        this.serviceAreaRepository = serviceAreaRepository;
        this.storeRepository = storeRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEntityType() {
        return ENTITY_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RecoverableEntityHandle> findHandle(String entityId) {
        UUID serviceAreaId = parseUuid(entityId);
        return serviceAreaRepository.findById(serviceAreaId).map(ServiceAreaHandle::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecoverableEntityHandle restoreHandle(String entityId, JsonNode snapshot) {
        UUID serviceAreaId = parseUuid(entityId);
        ServiceArea entity = serviceAreaRepository.findById(serviceAreaId).orElseGet(ServiceArea::new);
        entity.setId(serviceAreaId);
        ServiceAreaHandle handle = new ServiceAreaHandle(entity);
        handle.restoreFromSnapshot(snapshot, RecoveryLifecycleState.ACTIVE);
        return handle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hardDelete(String entityId) {
        UUID serviceAreaId = parseUuid(entityId);
        ServiceArea entity = serviceAreaRepository.findById(serviceAreaId)
                .orElseThrow(() -> new NotFoundException("SERVICE_AREA_NOT_FOUND", "Service area not found"));
        serviceAreaRepository.delete(entity);
    }

    /**
     * Parses a UUID from a governed entity id.
     *
     * @param entityId The governed entity id.
     * @return The parsed UUID.
     */
    private UUID parseUuid(String entityId) {
        try {
            return UUID.fromString(entityId);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("RECOVERY_ENTITY_ID_INVALID", "Invalid service area identifier: " + entityId);
        }
    }

    /**
     * Provides a recoverable-entity handle over a service-area aggregate.
     */
    private final class ServiceAreaHandle implements RecoverableEntityHandle {
        private final ServiceArea serviceArea;

        /**
         * Creates a new handle over a service-area aggregate.
         *
         * @param serviceArea The service-area aggregate.
         */
        private ServiceAreaHandle(ServiceArea serviceArea) {
            this.serviceArea = serviceArea;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getEntityId() {
            return String.valueOf(serviceArea.getId());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return serviceArea.getName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object toSnapshot() {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", serviceArea.getId());
            snapshot.put("name", serviceArea.getName());
            snapshot.put("type", serviceArea.getType());
            snapshot.put("status", serviceArea.getStatus());
            snapshot.put("centerLatitude", serviceArea.getCenterLatitude());
            snapshot.put("centerLongitude", serviceArea.getCenterLongitude());
            snapshot.put("radiusMeters", serviceArea.getRadiusMeters());
            snapshot.put("polygonGeoJson", serviceArea.getPolygonGeoJson());
            snapshot.put("rulesJson", serviceArea.getRulesJson());
            snapshot.put("storeIds", serviceArea.getStores().stream().map(Store::getId).map(UUID::toString).toList());
            return snapshot;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void applyLifecycleState(RecoveryLifecycleState state) {
            serviceArea.setStatus(state == RecoveryLifecycleState.ACTIVE ? ServiceAreaStatus.ACTIVE : ServiceAreaStatus.INACTIVE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void restoreFromSnapshot(JsonNode snapshot, RecoveryLifecycleState targetState) {
            serviceArea.setName(text(snapshot, "name"));
            serviceArea.setType(type(snapshot));
            serviceArea.setStatus(status(snapshot, targetState));
            serviceArea.setCenterLatitude(decimal(snapshot, "centerLatitude"));
            serviceArea.setCenterLongitude(decimal(snapshot, "centerLongitude"));
            serviceArea.setRadiusMeters(integer(snapshot, "radiusMeters"));
            serviceArea.setPolygonGeoJson(text(snapshot, "polygonGeoJson"));
            serviceArea.setRulesJson(text(snapshot, "rulesJson"));
            serviceArea.getStores().clear();
            serviceArea.getStores().addAll(resolveStores(snapshot));
            applyLifecycleState(targetState);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void persist() {
            serviceAreaRepository.save(serviceArea);
        }

        /**
         * Reads a text field from a service-area snapshot.
         *
         * @param snapshot The service-area snapshot payload.
         * @param fieldName The field name.
         * @return The field value.
         */
        private String text(JsonNode snapshot, String fieldName) {
            JsonNode field = snapshot.get(fieldName);
            return field == null || field.isNull() ? null : field.asText();
        }

        /**
         * Reads an integer field from a service-area snapshot.
         *
         * @param snapshot The service-area snapshot payload.
         * @param fieldName The field name.
         * @return The field value.
         */
        private Integer integer(JsonNode snapshot, String fieldName) {
            JsonNode field = snapshot.get(fieldName);
            return field == null || field.isNull() ? null : field.asInt();
        }

        /**
         * Reads a decimal field from a service-area snapshot.
         *
         * @param snapshot The service-area snapshot payload.
         * @param fieldName The field name.
         * @return The field value.
         */
        private BigDecimal decimal(JsonNode snapshot, String fieldName) {
            JsonNode field = snapshot.get(fieldName);
            return field == null || field.isNull() ? null : field.decimalValue();
        }

        /**
         * Reads a service-area type from a snapshot.
         *
         * @param snapshot The service-area snapshot payload.
         * @return The field value.
         */
        private ServiceAreaType type(JsonNode snapshot) {
            String value = text(snapshot, "type");
            return value == null ? ServiceAreaType.RADIUS : ServiceAreaType.valueOf(value);
        }

        /**
         * Resolves the effective service-area status from a snapshot and target lifecycle state.
         *
         * @param snapshot The service-area snapshot payload.
         * @param targetState The requested target lifecycle state.
         * @return The effective service-area status.
         */
        private ServiceAreaStatus status(JsonNode snapshot, RecoveryLifecycleState targetState) {
            if (targetState == RecoveryLifecycleState.ACTIVE) {
                return ServiceAreaStatus.ACTIVE;
            }
            String value = text(snapshot, "status");
            return value == null ? ServiceAreaStatus.INACTIVE : ServiceAreaStatus.valueOf(value);
        }

        /**
         * Resolves assigned stores from a snapshot payload.
         *
         * @param snapshot The service-area snapshot payload.
         * @return The resolved store set.
         */
        private Set<Store> resolveStores(JsonNode snapshot) {
            JsonNode field = snapshot.get("storeIds");
            if (field == null || !field.isArray()) {
                return Set.of();
            }
            List<UUID> storeIds = StreamSupport.stream(field.spliterator(), false)
                    .map(JsonNode::asText)
                    .map(UUID::fromString)
                    .toList();
            return storeIds.isEmpty() ? Set.of() : storeRepository.findAllById(storeIds).stream().collect(Collectors.toSet());
        }
    }
}
