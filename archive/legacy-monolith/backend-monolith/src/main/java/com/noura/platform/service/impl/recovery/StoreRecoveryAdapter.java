package com.noura.platform.service.impl.recovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.enums.RecoveryLifecycleState;
import com.noura.platform.domain.enums.StoreServiceType;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.service.recovery.RecoverableEntityAdapter;
import com.noura.platform.service.recovery.RecoverableEntityHandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Implements recoverable-entity behavior for storefront stores.
 */
@Component
public class StoreRecoveryAdapter implements RecoverableEntityAdapter {
    private static final String ENTITY_TYPE = "STORE";

    private final StoreRepository storeRepository;

    /**
     * Creates a new store recovery adapter.
     *
     * @param storeRepository The store repository.
     */
    public StoreRecoveryAdapter(StoreRepository storeRepository) {
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
        UUID storeId = parseUuid(entityId);
        return storeRepository.findById(storeId).map(StoreHandle::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecoverableEntityHandle restoreHandle(String entityId, JsonNode snapshot) {
        UUID storeId = parseUuid(entityId);
        Store store = storeRepository.findById(storeId).orElseGet(Store::new);
        store.setId(storeId);
        StoreHandle handle = new StoreHandle(store);
        handle.restoreFromSnapshot(snapshot, RecoveryLifecycleState.ACTIVE);
        return handle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hardDelete(String entityId) {
        UUID storeId = parseUuid(entityId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        storeRepository.delete(store);
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
            throw new BadRequestException("RECOVERY_ENTITY_ID_INVALID", "Invalid store identifier: " + entityId);
        }
    }

    /**
     * Provides a recoverable-entity handle over a store aggregate.
     */
    private final class StoreHandle implements RecoverableEntityHandle {
        private final Store store;

        /**
         * Creates a new handle over a store aggregate.
         *
         * @param store The store aggregate.
         */
        private StoreHandle(Store store) {
            this.store = store;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getEntityId() {
            return String.valueOf(store.getId());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return store.getName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object toSnapshot() {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", store.getId());
            snapshot.put("name", store.getName());
            snapshot.put("addressLine1", store.getAddressLine1());
            snapshot.put("city", store.getCity());
            snapshot.put("state", store.getState());
            snapshot.put("zipCode", store.getZipCode());
            snapshot.put("country", store.getCountry());
            snapshot.put("region", store.getRegion());
            snapshot.put("latitude", store.getLatitude());
            snapshot.put("longitude", store.getLongitude());
            snapshot.put("serviceRadiusMeters", store.getServiceRadiusMeters());
            snapshot.put("openTime", store.getOpenTime());
            snapshot.put("closeTime", store.getCloseTime());
            snapshot.put("active", store.isActive());
            snapshot.put("services", store.getServices());
            snapshot.put("shippingFee", store.getShippingFee());
            snapshot.put("freeShippingThreshold", store.getFreeShippingThreshold());
            return snapshot;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void applyLifecycleState(RecoveryLifecycleState state) {
            store.setActive(state == RecoveryLifecycleState.ACTIVE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void restoreFromSnapshot(JsonNode snapshot, RecoveryLifecycleState targetState) {
            store.setName(text(snapshot, "name"));
            store.setAddressLine1(text(snapshot, "addressLine1"));
            store.setCity(text(snapshot, "city"));
            store.setState(text(snapshot, "state"));
            store.setZipCode(text(snapshot, "zipCode"));
            store.setCountry(text(snapshot, "country"));
            store.setRegion(text(snapshot, "region"));
            store.setLatitude(decimal(snapshot, "latitude"));
            store.setLongitude(decimal(snapshot, "longitude"));
            store.setServiceRadiusMeters(integer(snapshot, "serviceRadiusMeters"));
            store.setOpenTime(time(snapshot, "openTime"));
            store.setCloseTime(time(snapshot, "closeTime"));
            store.setShippingFee(decimal(snapshot, "shippingFee"));
            store.setFreeShippingThreshold(decimal(snapshot, "freeShippingThreshold"));
            store.setServices(services(snapshot));
            applyLifecycleState(targetState);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void persist() {
            storeRepository.save(store);
        }

        /**
         * Reads a text field from a store snapshot.
         *
         * @param snapshot The store snapshot payload.
         * @param fieldName The field name.
         * @return The field value.
         */
        private String text(JsonNode snapshot, String fieldName) {
            JsonNode field = snapshot.get(fieldName);
            return field == null || field.isNull() ? null : field.asText();
        }

        /**
         * Reads an integer field from a store snapshot.
         *
         * @param snapshot The store snapshot payload.
         * @param fieldName The field name.
         * @return The field value.
         */
        private Integer integer(JsonNode snapshot, String fieldName) {
            JsonNode field = snapshot.get(fieldName);
            return field == null || field.isNull() ? null : field.asInt();
        }

        /**
         * Reads a decimal field from a store snapshot.
         *
         * @param snapshot The store snapshot payload.
         * @param fieldName The field name.
         * @return The field value.
         */
        private BigDecimal decimal(JsonNode snapshot, String fieldName) {
            JsonNode field = snapshot.get(fieldName);
            return field == null || field.isNull() ? null : field.decimalValue();
        }

        /**
         * Reads a time field from a store snapshot.
         *
         * @param snapshot The store snapshot payload.
         * @param fieldName The field name.
         * @return The field value.
         */
        private LocalTime time(JsonNode snapshot, String fieldName) {
            String value = text(snapshot, fieldName);
            return value == null ? null : LocalTime.parse(value);
        }

        /**
         * Reads service flags from a store snapshot.
         *
         * @param snapshot The store snapshot payload.
         * @return The store service set.
         */
        private Set<StoreServiceType> services(JsonNode snapshot) {
            JsonNode field = snapshot.get("services");
            if (field == null || field.isNull() || !field.isArray()) {
                return Set.of();
            }
            return StreamSupport.stream(field.spliterator(), false)
                    .map(JsonNode::asText)
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> StoreServiceType.valueOf(value.trim().toUpperCase(Locale.ROOT)))
                    .collect(Collectors.toSet());
        }
    }
}
