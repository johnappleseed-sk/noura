package com.noura.shipping.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.shipping.common.PageResponse;
import com.noura.shipping.domain.entity.MerchantRecord;
import com.noura.shipping.domain.entity.ServiceAreaRecord;
import com.noura.shipping.domain.entity.StoreRecord;
import com.noura.shipping.domain.enums.MerchantStatus;
import com.noura.shipping.domain.enums.ServiceAreaStatus;
import com.noura.shipping.domain.enums.ServiceAreaType;
import com.noura.shipping.domain.enums.StoreServiceType;
import com.noura.shipping.domain.enums.StoreStatus;
import com.noura.shipping.domain.enums.StoreType;
import com.noura.shipping.dto.network.MerchantCreateRequest;
import com.noura.shipping.dto.network.MerchantResponse;
import com.noura.shipping.dto.network.MerchantStatusUpdateRequest;
import com.noura.shipping.dto.network.ServiceAreaRequest;
import com.noura.shipping.dto.network.ServiceAreaResponse;
import com.noura.shipping.dto.network.ServiceAreaValidationRequest;
import com.noura.shipping.dto.network.ServiceEligibilityResponse;
import com.noura.shipping.dto.network.StoreLocationResponse;
import com.noura.shipping.dto.network.StoreLocationUpdateRequest;
import com.noura.shipping.dto.network.StoreRequest;
import com.noura.shipping.dto.network.StoreResponse;
import com.noura.shipping.dto.network.StoreStatusUpdateRequest;
import com.noura.shipping.exception.NotFoundException;
import com.noura.shipping.exception.ShippingOperationException;
import com.noura.shipping.repository.MerchantRecordRepository;
import com.noura.shipping.repository.ServiceAreaRecordRepository;
import com.noura.shipping.repository.StoreRecordRepository;
import com.noura.shipping.service.FulfillmentNetworkService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Shipping-owned compatibility implementation for merchant, store, and service-area admin flows.
 *
 * <p>The extracted platform does not yet run a dedicated network service, so this class provides
 * the minimum governed ownership needed by admin-web while keeping delivery geography close to
 * shipping-service, where the active store and coverage decisions already belong.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FulfillmentNetworkServiceImpl implements FulfillmentNetworkService {

    private final MerchantRecordRepository merchantRecordRepository;
    private final StoreRecordRepository storeRecordRepository;
    private final ServiceAreaRecordRepository serviceAreaRecordRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MerchantResponse> listMerchants(
            String search,
            MerchantStatus status,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        Page<MerchantResponse> result = merchantRecordRepository.findAll(merchantSpecification(search, status), pageRequest(page, size, sortBy, direction))
                .map(this::toMerchantResponse);
        return PageResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantResponse getMerchant(UUID merchantId) {
        return toMerchantResponse(requireMerchant(merchantId));
    }

    @Override
    public MerchantResponse createMerchant(MerchantCreateRequest request, String actorUserId) {
        merchantRecordRepository.findByMerchantCodeIgnoreCase(request.merchantCode().trim())
                .ifPresent(existing -> {
                    throw new ShippingOperationException(HttpStatus.CONFLICT, "MERCHANT_CODE_EXISTS", "Merchant code already exists");
                });

        MerchantRecord entity = new MerchantRecord();
        entity.setMerchantCode(request.merchantCode().trim().toUpperCase(Locale.ROOT));
        entity.setLegalName(request.legalName().trim());
        entity.setDisplayName(request.displayName().trim());
        entity.setEmail(normalize(request.email()));
        entity.setPhone(normalize(request.phone()));
        entity.setCountryCode(normalizeUpper(request.countryCode()));
        entity.setStatus(request.status() == null ? MerchantStatus.DRAFT : request.status());
        entity.setContractStartAt(parseDate(request.contractStartAt()));
        entity.setContractEndAt(parseDate(request.contractEndAt()));
        entity.setNotes(normalize(request.notes()));
        applyAudit(entity, actorUserId, true);

        return toMerchantResponse(merchantRecordRepository.save(entity));
    }

    @Override
    public MerchantResponse updateMerchantStatus(UUID merchantId, MerchantStatusUpdateRequest request, String actorUserId) {
        MerchantRecord entity = requireMerchant(merchantId);
        entity.setStatus(request.status());
        applyAudit(entity, actorUserId, false);
        return toMerchantResponse(merchantRecordRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StoreResponse> listAdminStores(
            String search,
            UUID merchantId,
            StoreType type,
            StoreStatus status,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        Page<StoreResponse> result = storeRecordRepository.findAll(
                        storeSpecification(search, merchantId, type, status, false, null, null),
                        pageRequest(page, size, sortBy, direction)
                )
                .map(store -> toStoreResponse(store, null));
        return PageResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getAdminStore(UUID storeId) {
        return toStoreResponse(requireStore(storeId), null);
    }

    @Override
    public StoreResponse createAdminStore(StoreRequest request, String actorUserId) {
        return createOrUpdateStore(null, request, actorUserId);
    }

    @Override
    public StoreResponse updateStoreStatus(UUID storeId, StoreStatusUpdateRequest request, String actorUserId) {
        StoreRecord entity = requireStore(storeId);
        entity.setStatus(request.status());
        applyAudit(entity, actorUserId, false);
        return toStoreResponse(storeRecordRepository.save(entity), null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StoreResponse> listStores(
            String service,
            Boolean openNow,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        Pageable pageable = pageRequest(page, size, sortBy, direction);
        List<StoreRecord> filtered = storeRecordRepository.findByDeletedAtIsNullAndStatus(StoreStatus.ACTIVE).stream()
                .filter(store -> openNow == null || store.isOpenNow() == openNow)
                .filter(store -> supportsService(store, service))
                .sorted(storeComparator(pageable.getSort()))
                .toList();
        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<StoreResponse> content = filtered.subList(fromIndex, toIndex).stream()
                .map(store -> toStoreResponse(store, null))
                .toList();
        Page<StoreResponse> result = new org.springframework.data.domain.PageImpl<>(content, pageable, filtered.size());
        return PageResponse.from(result);
    }

    @Override
    public StoreResponse createStore(StoreRequest request, String actorUserId) {
        return createOrUpdateStore(null, request, actorUserId);
    }

    @Override
    public StoreResponse updateStore(UUID storeId, StoreRequest request, String actorUserId) {
        return createOrUpdateStore(storeId, request, actorUserId);
    }

    @Override
    public void deleteStore(UUID storeId, String actorUserId) {
        StoreRecord entity = requireStore(storeId);
        entity.setDeletedAt(Instant.now());
        entity.setStatus(StoreStatus.CLOSED);
        applyAudit(entity, actorUserId, false);
        storeRecordRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreLocationResponse getStoreLocation(UUID storeId) {
        StoreRecord entity = requireStore(storeId);
        return new StoreLocationResponse(
                entity.getId(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getAddressLine1(),
                entity.getAddressLine2(),
                entity.getCity(),
                entity.getPostalCode(),
                entity.getCountryCode()
        );
    }

    @Override
    public StoreLocationResponse updateStoreLocation(UUID storeId, StoreLocationUpdateRequest request, String actorUserId) {
        StoreRecord entity = requireStore(storeId);
        entity.setLatitude(request.latitude());
        entity.setLongitude(request.longitude());
        entity.setAddressLine1(normalize(request.addressLine1()));
        entity.setAddressLine2(normalize(request.addressLine2()));
        entity.setCity(normalize(request.city()));
        entity.setPostalCode(normalize(request.postalCode()));
        entity.setCountryCode(normalizeUpper(request.countryCode()));
        applyAudit(entity, actorUserId, false);
        StoreRecord saved = storeRecordRepository.save(entity);
        return new StoreLocationResponse(
                saved.getId(),
                saved.getLatitude(),
                saved.getLongitude(),
                saved.getAddressLine1(),
                saved.getAddressLine2(),
                saved.getCity(),
                saved.getPostalCode(),
                saved.getCountryCode()
        );
    }

    @Override
    public void setPreferredStore(UUID storeId, String actorUserId) {
        StoreRecord target = requireStore(storeId);
        List<StoreRecord> stores = storeRecordRepository.findAll();
        for (StoreRecord store : stores) {
            boolean preferred = Objects.equals(store.getId(), target.getId()) && store.getDeletedAt() == null;
            store.setPreferredStore(preferred);
            applyAudit(store, actorUserId, false);
        }
        storeRecordRepository.saveAll(stores);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreResponse> findNearestStores(BigDecimal latitude, BigDecimal longitude, int limit) {
        return storeRecordRepository.findByDeletedAtIsNullAndStatus(StoreStatus.ACTIVE).stream()
                .filter(store -> store.getLatitude() != null && store.getLongitude() != null)
                .map(store -> toStoreResponse(store, distanceMeters(latitude, longitude, store.getLatitude(), store.getLongitude())))
                .sorted(Comparator.comparing(response -> response.distanceMeters() == null ? Long.MAX_VALUE : response.distanceMeters()))
                .limit(Math.max(limit, 1))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ServiceAreaResponse> listServiceAreas(
            String query,
            ServiceAreaStatus status,
            ServiceAreaType type,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        Page<ServiceAreaResponse> result = serviceAreaRecordRepository.findAll(
                        serviceAreaSpecification(query, status, type),
                        pageRequest(page, size, sortBy, direction)
                )
                .map(this::toServiceAreaResponse);
        return PageResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceAreaResponse getServiceArea(UUID serviceAreaId) {
        return toServiceAreaResponse(requireServiceArea(serviceAreaId));
    }

    @Override
    public ServiceAreaResponse createServiceArea(ServiceAreaRequest request, String actorUserId) {
        validateServiceAreaGeometry(request);
        ServiceAreaRecord entity = new ServiceAreaRecord();
        applyServiceAreaFields(entity, request);
        applyAudit(entity, actorUserId, true);
        return toServiceAreaResponse(serviceAreaRecordRepository.save(entity));
    }

    @Override
    public ServiceAreaResponse updateServiceArea(UUID serviceAreaId, ServiceAreaRequest request, String actorUserId) {
        validateServiceAreaGeometry(request);
        ServiceAreaRecord entity = requireServiceArea(serviceAreaId);
        applyServiceAreaFields(entity, request);
        applyAudit(entity, actorUserId, false);
        return toServiceAreaResponse(serviceAreaRecordRepository.save(entity));
    }

    @Override
    public void deleteServiceArea(UUID serviceAreaId, String actorUserId) {
        ServiceAreaRecord entity = requireServiceArea(serviceAreaId);
        entity.setDeletedAt(Instant.now());
        entity.setStatus(ServiceAreaStatus.INACTIVE);
        applyAudit(entity, actorUserId, false);
        serviceAreaRecordRepository.save(entity);
    }

    @Override
    public ServiceAreaResponse activateServiceArea(UUID serviceAreaId, String actorUserId) {
        ServiceAreaRecord entity = requireServiceArea(serviceAreaId);
        entity.setStatus(ServiceAreaStatus.ACTIVE);
        applyAudit(entity, actorUserId, false);
        return toServiceAreaResponse(serviceAreaRecordRepository.save(entity));
    }

    @Override
    public ServiceAreaResponse deactivateServiceArea(UUID serviceAreaId, String actorUserId) {
        ServiceAreaRecord entity = requireServiceArea(serviceAreaId);
        entity.setStatus(ServiceAreaStatus.INACTIVE);
        applyAudit(entity, actorUserId, false);
        return toServiceAreaResponse(serviceAreaRecordRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceEligibilityResponse validateServiceArea(ServiceAreaValidationRequest request) {
        List<ServiceAreaRecord> activeAreas = serviceAreaRecordRepository.findByDeletedAtIsNullAndStatus(ServiceAreaStatus.ACTIVE);
        List<MatchCandidate> candidates = activeAreas.stream()
                .map(area -> matchArea(area, request.latitude(), request.longitude()))
                .filter(MatchCandidate::insideArea)
                .sorted(Comparator.comparing(candidate -> candidate.distanceMeters() == null ? Long.MAX_VALUE : candidate.distanceMeters()))
                .toList();

        if (candidates.isEmpty()) {
            return new ServiceEligibilityResponse(
                    false,
                    request.serviceType(),
                    null,
                    null,
                    null,
                    false,
                    false,
                    "OUT_OF_RANGE"
            );
        }

        MatchCandidate areaMatch = candidates.getFirst();
        List<StoreRecord> eligibleStores = resolveAreaStores(areaMatch.area()).stream()
                .filter(store -> store.getDeletedAt() == null)
                .filter(store -> store.getStatus() == StoreStatus.ACTIVE)
                .filter(store -> store.getSupportedServices() == null || store.getSupportedServices().isEmpty() || store.getSupportedServices().contains(request.serviceType()))
                .toList();

        if (eligibleStores.isEmpty()) {
            return new ServiceEligibilityResponse(
                    false,
                    request.serviceType(),
                    areaMatch.area().getId(),
                    null,
                    areaMatch.distanceMeters(),
                    true,
                    false,
                    "NO_ACTIVE_STORE"
            );
        }

        StoreRecord matchedStore = eligibleStores.stream()
                .min(Comparator.comparing(store -> distanceOrMax(request.latitude(), request.longitude(), store.getLatitude(), store.getLongitude())))
                .orElseThrow();
        long distanceMeters = distanceOrMax(request.latitude(), request.longitude(), matchedStore.getLatitude(), matchedStore.getLongitude());

        if (request.maxDistanceMeters() != null && distanceMeters > request.maxDistanceMeters()) {
            return new ServiceEligibilityResponse(
                    false,
                    request.serviceType(),
                    areaMatch.area().getId(),
                    matchedStore.getId(),
                    distanceMeters,
                    true,
                    matchedStore.isOpenNow(),
                    "OUT_OF_RANGE"
            );
        }
        if (!matchedStore.isOpenNow()) {
            return new ServiceEligibilityResponse(
                    false,
                    request.serviceType(),
                    areaMatch.area().getId(),
                    matchedStore.getId(),
                    distanceMeters,
                    true,
                    false,
                    "STORE_CLOSED"
            );
        }

        return new ServiceEligibilityResponse(
                true,
                request.serviceType(),
                areaMatch.area().getId(),
                matchedStore.getId(),
                distanceMeters,
                true,
                true,
                "MATCHED"
        );
    }

    private MerchantRecord requireMerchant(UUID merchantId) {
        return merchantRecordRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("MERCHANT_NOT_FOUND", "Merchant not found"));
    }

    private StoreRecord requireStore(UUID storeId) {
        return storeRecordRepository.findById(storeId)
                .filter(store -> store.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
    }

    private ServiceAreaRecord requireServiceArea(UUID serviceAreaId) {
        return serviceAreaRecordRepository.findById(serviceAreaId)
                .filter(area -> area.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("SERVICE_AREA_NOT_FOUND", "Service area not found"));
    }

    private StoreResponse createOrUpdateStore(UUID storeId, StoreRequest request, String actorUserId) {
        if (request.merchantId() != null) {
            requireMerchant(request.merchantId());
        }

        StoreRecord entity = storeId == null ? new StoreRecord() : requireStore(storeId);
        String requestedCode = normalizeUpper(request.storeCode());
        if (storeId == null) {
            entity.setStoreCode(requestedCode == null ? generateStoreCode(request.name()) : requestedCode);
        } else if (requestedCode != null) {
            entity.setStoreCode(requestedCode);
        }

        storeRecordRepository.findByStoreCodeIgnoreCase(entity.getStoreCode())
                .filter(existing -> !existing.getId().equals(entity.getId()))
                .ifPresent(existing -> {
                    throw new ShippingOperationException(HttpStatus.CONFLICT, "STORE_CODE_EXISTS", "Store code already exists");
                });

        entity.setName(request.name().trim());
        entity.setSlug(normalizeSlug(request.slug(), request.name()));
        entity.setMerchantId(request.merchantId());
        entity.setType(request.type());
        entity.setStatus(request.status() == null ? defaultStoreStatus(storeId) : request.status());
        entity.setCountryCode(normalizeUpper(request.countryCode()));
        entity.setCity(normalize(request.city()));
        entity.setAddressLine1(normalize(request.addressLine1()));
        entity.setAddressLine2(normalize(request.addressLine2()));
        entity.setPostalCode(normalize(request.postalCode()));
        entity.setContactEmail(normalize(request.contactEmail()));
        entity.setContactPhone(normalize(request.contactPhone()));
        entity.setLatitude(request.latitude());
        entity.setLongitude(request.longitude());
        entity.setOpenNow(request.openNow() == null || request.openNow());
        entity.setSupportedServices(normalizeServices(request.supportedServices()));
        applyAudit(entity, actorUserId, storeId == null);

        return toStoreResponse(storeRecordRepository.save(entity), null);
    }

    private void applyServiceAreaFields(ServiceAreaRecord entity, ServiceAreaRequest request) {
        entity.setName(request.name().trim());
        entity.setType(request.type());
        entity.setStatus(request.status() == null ? ServiceAreaStatus.ACTIVE : request.status());
        entity.setCenterLatitude(request.centerLatitude());
        entity.setCenterLongitude(request.centerLongitude());
        entity.setRadiusMeters(request.radiusMeters());
        entity.setPolygonGeoJson(normalize(request.polygonGeoJson()));
        entity.setRulesJson(normalize(request.rulesJson()));
        entity.setStoreIds(request.storeIds() == null ? new ArrayList<>() : new ArrayList<>(request.storeIds()));
    }

    private void validateServiceAreaGeometry(ServiceAreaRequest request) {
        if (request.type() == ServiceAreaType.RADIUS) {
            if (request.centerLatitude() == null || request.centerLongitude() == null || request.radiusMeters() == null || request.radiusMeters() <= 0) {
                throw new ShippingOperationException(
                        HttpStatus.BAD_REQUEST,
                        "SERVICE_AREA_RADIUS_INVALID",
                        "Radius service areas require center latitude, center longitude, and a positive radius"
                );
            }
        }
        if (request.type() == ServiceAreaType.POLYGON && normalize(request.polygonGeoJson()) == null) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "SERVICE_AREA_POLYGON_REQUIRED",
                    "Polygon service areas require polygon GeoJSON"
            );
        }
    }

    private MerchantResponse toMerchantResponse(MerchantRecord entity) {
        return new MerchantResponse(
                entity.getId(),
                entity.getMerchantCode(),
                entity.getLegalName(),
                entity.getDisplayName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getCountryCode(),
                entity.getStatus(),
                entity.getContractStartAt(),
                entity.getContractEndAt(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private StoreResponse toStoreResponse(StoreRecord entity, Long distanceMeters) {
        return new StoreResponse(
                entity.getId(),
                entity.getStoreCode(),
                entity.getName(),
                entity.getSlug(),
                entity.getMerchantId(),
                entity.getType(),
                entity.getStatus(),
                entity.getCountryCode(),
                entity.getCity(),
                entity.getAddressLine1(),
                entity.getAddressLine2(),
                entity.getPostalCode(),
                entity.getContactEmail(),
                entity.getContactPhone(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.isOpenNow(),
                entity.isPreferredStore(),
                entity.getSupportedServices() == null ? List.of() : List.copyOf(entity.getSupportedServices()),
                distanceMeters,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ServiceAreaResponse toServiceAreaResponse(ServiceAreaRecord entity) {
        return new ServiceAreaResponse(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getStatus(),
                entity.getCenterLatitude(),
                entity.getCenterLongitude(),
                entity.getRadiusMeters(),
                entity.getPolygonGeoJson(),
                entity.getRulesJson(),
                entity.getStoreIds() == null ? List.of() : List.copyOf(entity.getStoreIds()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private Specification<MerchantRecord> merchantSpecification(String search, MerchantStatus status) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (normalize(search) != null) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("merchantCode")), like),
                        builder.like(builder.lower(root.get("legalName")), like),
                        builder.like(builder.lower(root.get("displayName")), like)
                ));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<StoreRecord> storeSpecification(
            String search,
            UUID merchantId,
            StoreType type,
            StoreStatus status,
            boolean publicOnly,
            String service,
            Boolean openNow
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isNull(root.get("deletedAt")));
            if (normalize(search) != null) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("storeCode")), like),
                        builder.like(builder.lower(root.get("name")), like),
                        builder.like(builder.lower(root.get("slug")), like)
                ));
            }
            if (merchantId != null) {
                predicates.add(builder.equal(root.get("merchantId"), merchantId));
            }
            if (type != null) {
                predicates.add(builder.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (publicOnly) {
                predicates.add(builder.equal(root.get("status"), StoreStatus.ACTIVE));
            }
            if (openNow != null) {
                predicates.add(builder.equal(root.get("openNow"), openNow));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<ServiceAreaRecord> serviceAreaSpecification(
            String search,
            ServiceAreaStatus status,
            ServiceAreaType type
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isNull(root.get("deletedAt")));
            if (normalize(search) != null) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.like(builder.lower(root.get("name")), like));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (type != null) {
                predicates.add(builder.equal(root.get("type"), type));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable pageRequest(int page, int size, String sortBy, String direction) {
        String safeSort = normalize(sortBy);
        if (safeSort == null) {
            safeSort = "updatedAt";
        }
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(sortDirection, safeSort));
    }

    private void applyAudit(com.noura.shipping.domain.entity.AuditableEntity entity, String actorUserId, boolean create) {
        String actor = normalize(actorUserId) == null ? "system" : actorUserId.trim();
        if (create && entity.getCreatedBy() == null) {
            entity.setCreatedBy(actor);
        }
        entity.setUpdatedBy(actor);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private Instant parseDate(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (RuntimeException ex) {
            throw new ShippingOperationException(HttpStatus.BAD_REQUEST, "DATE_INVALID", "Invalid date value");
        }
    }

    private String generateStoreCode(String name) {
        String prefix = normalize(name) == null ? "STORE" : normalize(name).replaceAll("[^A-Za-z0-9]+", "").toUpperCase(Locale.ROOT);
        prefix = prefix.isBlank() ? "STORE" : prefix.substring(0, Math.min(prefix.length(), 6));
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String normalizeSlug(String slug, String name) {
        String source = normalize(slug);
        if (source == null) {
            source = normalize(name);
        }
        if (source == null) {
            return null;
        }
        return source.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private StoreStatus defaultStoreStatus(UUID storeId) {
        return storeId == null ? StoreStatus.DRAFT : requireStore(storeId).getStatus();
    }

    private List<StoreServiceType> normalizeServices(List<StoreServiceType> services) {
        if (services == null || services.isEmpty()) {
            return new ArrayList<>(List.of(StoreServiceType.DELIVERY, StoreServiceType.PICKUP));
        }
        return new ArrayList<>(services.stream().distinct().toList());
    }

    private boolean supportsService(StoreRecord store, String service) {
        String normalizedService = normalizeUpper(service);
        if (normalizedService == null) {
            return true;
        }
        if (store.getSupportedServices() == null || store.getSupportedServices().isEmpty()) {
            return true;
        }
        return store.getSupportedServices().stream()
                .map(Enum::name)
                .anyMatch(normalizedService::equals);
    }

    private Comparator<StoreRecord> storeComparator(Sort sort) {
        Sort.Order order = sort.stream().findFirst().orElse(Sort.Order.asc("name"));
        Comparator<StoreRecord> comparator;
        comparator = switch (order.getProperty()) {
            case "createdAt" -> Comparator.comparing(StoreRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "updatedAt" -> Comparator.comparing(StoreRecord::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "city" -> Comparator.comparing(store -> safeText(store.getCity()));
            case "storeCode" -> Comparator.comparing(store -> safeText(store.getStoreCode()));
            default -> Comparator.comparing(store -> safeText(store.getName()));
        };
        return order.isAscending() ? comparator : comparator.reversed();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private List<StoreRecord> resolveAreaStores(ServiceAreaRecord area) {
        List<UUID> storeIds = area.getStoreIds();
        if (storeIds == null || storeIds.isEmpty()) {
            return storeRecordRepository.findByDeletedAtIsNullAndStatus(StoreStatus.ACTIVE);
        }
        return storeIds.stream()
                .map(storeRecordRepository::findById)
                .flatMap(java.util.Optional::stream)
                .filter(store -> store.getDeletedAt() == null)
                .toList();
    }

    private MatchCandidate matchArea(ServiceAreaRecord area, BigDecimal latitude, BigDecimal longitude) {
        return switch (area.getType()) {
            case RADIUS -> matchRadiusArea(area, latitude, longitude);
            case POLYGON -> matchPolygonArea(area, latitude, longitude);
            case CITY, DISTRICT -> new MatchCandidate(area, false, null);
        };
    }

    private MatchCandidate matchRadiusArea(ServiceAreaRecord area, BigDecimal latitude, BigDecimal longitude) {
        if (area.getCenterLatitude() == null || area.getCenterLongitude() == null || area.getRadiusMeters() == null) {
            return new MatchCandidate(area, false, null);
        }
        long distance = distanceOrMax(latitude, longitude, area.getCenterLatitude(), area.getCenterLongitude());
        return new MatchCandidate(area, distance <= area.getRadiusMeters(), distance);
    }

    private MatchCandidate matchPolygonArea(ServiceAreaRecord area, BigDecimal latitude, BigDecimal longitude) {
        String geoJson = normalize(area.getPolygonGeoJson());
        if (geoJson == null) {
            return new MatchCandidate(area, false, null);
        }
        try {
            JsonNode root = objectMapper.readTree(geoJson);
            JsonNode coordinates = root.path("coordinates").path(0);
            if (!coordinates.isArray() || coordinates.isEmpty()) {
                return new MatchCandidate(area, false, null);
            }
            List<Point> points = new ArrayList<>();
            for (JsonNode coordinate : coordinates) {
                if (coordinate.isArray() && coordinate.size() >= 2) {
                    points.add(new Point(coordinate.get(1).asDouble(), coordinate.get(0).asDouble()));
                }
            }
            if (points.size() < 3) {
                return new MatchCandidate(area, false, null);
            }
            boolean inside = pointInPolygon(latitude.doubleValue(), longitude.doubleValue(), points);
            return new MatchCandidate(area, inside, inside ? 0L : null);
        } catch (Exception ex) {
            log.warn("Failed to parse polygon service area {}: {}", area.getId(), ex.getMessage());
            return new MatchCandidate(area, false, null);
        }
    }

    private boolean pointInPolygon(double latitude, double longitude, List<Point> points) {
        boolean inside = false;
        for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            Point a = points.get(i);
            Point b = points.get(j);
            boolean intersects = ((a.latitude() > latitude) != (b.latitude() > latitude))
                    && (longitude < (b.longitude() - a.longitude()) * (latitude - a.latitude()) / (b.latitude() - a.latitude()) + a.longitude());
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    private Long distanceMeters(BigDecimal originLatitude, BigDecimal originLongitude, BigDecimal latitude, BigDecimal longitude) {
        if (originLatitude == null || originLongitude == null || latitude == null || longitude == null) {
            return null;
        }
        return haversine(originLatitude.doubleValue(), originLongitude.doubleValue(), latitude.doubleValue(), longitude.doubleValue());
    }

    private long distanceOrMax(BigDecimal originLatitude, BigDecimal originLongitude, BigDecimal latitude, BigDecimal longitude) {
        Long distance = distanceMeters(originLatitude, originLongitude, latitude, longitude);
        return distance == null ? Long.MAX_VALUE : distance;
    }

    private long haversine(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusMeters = 6_371_000.0d;
        double latitudeDelta = Math.toRadians(lat2 - lat1);
        double longitudeDelta = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(earthRadiusMeters * c).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private record MatchCandidate(
            ServiceAreaRecord area,
            boolean insideArea,
            Long distanceMeters
    ) {
    }

    private record Point(double latitude, double longitude) {
    }
}
