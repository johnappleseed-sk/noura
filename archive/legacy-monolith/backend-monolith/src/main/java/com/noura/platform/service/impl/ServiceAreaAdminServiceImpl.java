package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.ServiceArea;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryLifecycleState;
import com.noura.platform.domain.enums.ServiceAreaStatus;
import com.noura.platform.domain.enums.ServiceAreaType;
import com.noura.platform.dto.location.ServiceAreaDto;
import com.noura.platform.dto.location.ServiceAreaRequest;
import com.noura.platform.dto.recovery.RecoveryActionRequest;
import com.noura.platform.location.util.GeoJsonUtils;
import com.noura.platform.repository.ServiceAreaRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.service.OptionalCommerceAuditService;
import com.noura.platform.service.ServiceAreaAdminService;
import com.noura.platform.service.recovery.RecoveryGovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implements governed service-area administration workflows.
 */
@Service
@RequiredArgsConstructor
public class ServiceAreaAdminServiceImpl implements ServiceAreaAdminService {

    private final ServiceAreaRepository serviceAreaRepository;
    private final StoreRepository storeRepository;
    private final OptionalCommerceAuditService auditEventService;
    private final RecoveryGovernanceService recoveryGovernanceService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ServiceAreaDto> list(String query, ServiceAreaStatus status, ServiceAreaType type, Pageable pageable) {
        Specification<ServiceArea> spec = Specification.where(null);
        if (query != null && !query.isBlank()) {
            String normalized = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), normalized));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (type != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("type"), type));
        }
        Page<ServiceAreaDto> page = serviceAreaRepository.findAll(spec, pageable).map(this::toDto);
        Map<String, RecoveryLifecycleState> states = recoveryGovernanceService.resolveLifecycleStates(
                "SERVICE_AREA",
                page.getContent().stream().map(ServiceAreaDto::id).map(UUID::toString).toList()
        );
        List<ServiceAreaDto> filteredContent = page.getContent().stream()
                .filter(dto -> {
                    RecoveryLifecycleState state = states.get(dto.id().toString());
                    return state == null || !Set.of(RecoveryLifecycleState.TRASHED, RecoveryLifecycleState.PURGED, RecoveryLifecycleState.ANONYMIZED).contains(state);
                })
                .toList();
        return new PageImpl<>(filteredContent, pageable, filteredContent.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceAreaDto get(UUID serviceAreaId) {
        return toDto(require(serviceAreaId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceAreaDto create(ServiceAreaRequest request, String actor) {
        ServiceArea entity = new ServiceArea();
        apply(entity, request);
        ServiceArea saved = serviceAreaRepository.save(entity);
        auditEventService.record(
                "SERVICE_AREA_CREATED",
                "ServiceArea",
                saved.getId(),
                null,
                toDto(saved),
                Collections.singletonMap("actor", actor)
        );
        recoveryGovernanceService.captureVersion("SERVICE_AREA", saved.getId().toString(), RecoveryActionType.CREATE, actor, "Service area created.", Map.of());
        return toDto(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceAreaDto update(UUID serviceAreaId, ServiceAreaRequest request, String actor) {
        ServiceArea existing = require(serviceAreaId);
        ServiceAreaDto before = toDto(existing);
        apply(existing, request);
        ServiceArea saved = serviceAreaRepository.save(existing);
        auditEventService.record(
                "SERVICE_AREA_UPDATED",
                "ServiceArea",
                saved.getId(),
                before,
                toDto(saved),
                Collections.singletonMap("actor", actor)
        );
        recoveryGovernanceService.captureVersion("SERVICE_AREA", saved.getId().toString(), RecoveryActionType.UPDATE, actor, "Service area updated.", Map.of());
        return toDto(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID serviceAreaId, String actor) {
        ServiceAreaDto before = toDto(require(serviceAreaId));
        recoveryGovernanceService.applyAction(
                new RecoveryActionRequest("SERVICE_AREA", serviceAreaId.toString(), RecoveryActionType.TRASH, "Service area moved to trash.", null, null, null, Map.of()),
                actor
        );
        auditEventService.record("SERVICE_AREA_TRASHED", "ServiceArea", serviceAreaId, before, null, Collections.singletonMap("actor", actor));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceAreaDto activate(UUID serviceAreaId, String actor) {
        ServiceAreaDto before = toDto(require(serviceAreaId));
        recoveryGovernanceService.applyAction(
                new RecoveryActionRequest("SERVICE_AREA", serviceAreaId.toString(), RecoveryActionType.ACTIVATE, "Service area activated.", null, null, null, Map.of()),
                actor
        );
        ServiceArea saved = require(serviceAreaId);
        auditEventService.record("SERVICE_AREA_ACTIVATED", "ServiceArea", serviceAreaId, before, toDto(saved), Collections.singletonMap("actor", actor));
        return toDto(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceAreaDto deactivate(UUID serviceAreaId, String actor) {
        ServiceAreaDto before = toDto(require(serviceAreaId));
        recoveryGovernanceService.applyAction(
                new RecoveryActionRequest("SERVICE_AREA", serviceAreaId.toString(), RecoveryActionType.DEACTIVATE, "Service area deactivated.", null, null, null, Map.of()),
                actor
        );
        ServiceArea saved = require(serviceAreaId);
        auditEventService.record("SERVICE_AREA_DEACTIVATED", "ServiceArea", serviceAreaId, before, toDto(saved), Collections.singletonMap("actor", actor));
        return toDto(saved);
    }

    /**
     * Retrieves a required service-area aggregate.
     *
     * @param id The service-area identifier.
     * @return The resolved service-area aggregate.
     */
    private ServiceArea require(UUID id) {
        return serviceAreaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SERVICE_AREA_NOT_FOUND", "Service area not found"));
    }

    /**
     * Applies a request payload to a service-area aggregate.
     *
     * @param entity The service-area aggregate to mutate.
     * @param request The request payload.
     */
    private void apply(ServiceArea entity, ServiceAreaRequest request) {
        if (request == null) {
            throw new BadRequestException("SERVICE_AREA_INVALID", "Request payload is required.");
        }

        ServiceAreaType type = request.type();
        if (type == null) {
            throw new BadRequestException("SERVICE_AREA_TYPE_REQUIRED", "Service area type is required.");
        }

        entity.setName(request.name() == null ? null : request.name().trim());
        entity.setType(type);
        entity.setStatus(request.status() == null ? ServiceAreaStatus.ACTIVE : request.status());
        entity.setRulesJson(request.rulesJson());

        // Type-specific fields and validations.
        switch (type) {
            case RADIUS -> {
                if (request.centerLatitude() == null || request.centerLongitude() == null || request.radiusMeters() == null || request.radiusMeters() <= 0) {
                    throw new BadRequestException("SERVICE_AREA_RADIUS_INVALID", "Radius areas require centerLatitude, centerLongitude, and a positive radiusMeters.");
                }
                entity.setCenterLatitude(request.centerLatitude());
                entity.setCenterLongitude(request.centerLongitude());
                entity.setRadiusMeters(request.radiusMeters());
                entity.setPolygonGeoJson(null);
            }
            case POLYGON -> {
                if (request.polygonGeoJson() == null || request.polygonGeoJson().isBlank()) {
                    throw new BadRequestException("SERVICE_AREA_POLYGON_INVALID", "Polygon areas require polygonGeoJson.");
                }
                if (GeoJsonUtils.extractOuterRing(request.polygonGeoJson()).isEmpty()) {
                    throw new BadRequestException("SERVICE_AREA_POLYGON_INVALID", "polygonGeoJson must be a valid GeoJSON Polygon or MultiPolygon.");
                }
                entity.setPolygonGeoJson(request.polygonGeoJson());
                entity.setCenterLatitude(request.centerLatitude());
                entity.setCenterLongitude(request.centerLongitude());
                entity.setRadiusMeters(null);
            }
            case CITY, DISTRICT -> {
                // CITY/DISTRICT matching uses name as the default matcher (case-insensitive).
                if (entity.getName() == null || entity.getName().isBlank()) {
                    throw new BadRequestException("SERVICE_AREA_NAME_REQUIRED", "City/District service areas require a name.");
                }
                entity.setCenterLatitude(null);
                entity.setCenterLongitude(null);
                entity.setRadiusMeters(null);
                entity.setPolygonGeoJson(null);
            }
        }

        // Store assignments.
        List<UUID> storeIds = request.storeIds() == null ? List.of() : request.storeIds().stream().distinct().toList();
        if (storeIds.isEmpty()) {
            entity.getStores().clear();
        } else {
            List<Store> stores = storeRepository.findAllById(storeIds);
            if (stores.size() != storeIds.size()) {
                throw new BadRequestException("SERVICE_AREA_STORE_INVALID", "One or more storeIds are invalid.");
            }
            entity.getStores().clear();
            entity.getStores().addAll(stores);
        }
    }

    /**
     * Maps a service-area aggregate to the admin DTO.
     *
     * @param entity The service-area aggregate.
     * @return The mapped service-area DTO.
     */
    private ServiceAreaDto toDto(ServiceArea entity) {
        List<UUID> storeIds = entity.getStores() == null ? List.of() : entity.getStores().stream().map(Store::getId).toList();
        return new ServiceAreaDto(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getStatus(),
                entity.getCenterLatitude(),
                entity.getCenterLongitude(),
                entity.getRadiusMeters(),
                entity.getPolygonGeoJson(),
                entity.getRulesJson(),
                storeIds,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy()
        );
    }
}
