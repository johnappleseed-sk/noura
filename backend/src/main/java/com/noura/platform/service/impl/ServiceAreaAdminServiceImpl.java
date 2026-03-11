package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.ServiceArea;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.enums.ServiceAreaStatus;
import com.noura.platform.domain.enums.ServiceAreaType;
import com.noura.platform.dto.location.ServiceAreaDto;
import com.noura.platform.dto.location.ServiceAreaRequest;
import com.noura.platform.location.util.GeoJsonUtils;
import com.noura.platform.repository.ServiceAreaRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.service.OptionalCommerceAuditService;
import com.noura.platform.service.ServiceAreaAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceAreaAdminServiceImpl implements ServiceAreaAdminService {

    private final ServiceAreaRepository serviceAreaRepository;
    private final StoreRepository storeRepository;
    private final OptionalCommerceAuditService auditEventService;

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
        return serviceAreaRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceAreaDto get(UUID serviceAreaId) {
        return toDto(require(serviceAreaId));
    }

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
        return toDto(saved);
    }

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
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID serviceAreaId, String actor) {
        ServiceArea existing = require(serviceAreaId);
        ServiceAreaDto before = toDto(existing);
        serviceAreaRepository.delete(existing);
        auditEventService.record(
                "SERVICE_AREA_DELETED",
                "ServiceArea",
                serviceAreaId,
                before,
                null,
                Collections.singletonMap("actor", actor)
        );
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceAreaDto activate(UUID serviceAreaId, String actor) {
        ServiceArea existing = require(serviceAreaId);
        ServiceAreaDto before = toDto(existing);
        existing.setStatus(ServiceAreaStatus.ACTIVE);
        ServiceArea saved = serviceAreaRepository.save(existing);
        auditEventService.record(
                "SERVICE_AREA_ACTIVATED",
                "ServiceArea",
                serviceAreaId,
                before,
                toDto(saved),
                Collections.singletonMap("actor", actor)
        );
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceAreaDto deactivate(UUID serviceAreaId, String actor) {
        ServiceArea existing = require(serviceAreaId);
        ServiceAreaDto before = toDto(existing);
        existing.setStatus(ServiceAreaStatus.INACTIVE);
        ServiceArea saved = serviceAreaRepository.save(existing);
        auditEventService.record(
                "SERVICE_AREA_DEACTIVATED",
                "ServiceArea",
                serviceAreaId,
                before,
                toDto(saved),
                Collections.singletonMap("actor", actor)
        );
        return toDto(saved);
    }

    private ServiceArea require(UUID id) {
        return serviceAreaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SERVICE_AREA_NOT_FOUND", "Service area not found"));
    }

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
