package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Merchant;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.StoreTenant;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.MerchantContractStatus;
import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.StoreServiceType;
import com.noura.platform.domain.enums.StoreStatus;
import com.noura.platform.domain.enums.StoreTenantStatus;
import com.noura.platform.domain.enums.StoreType;
import com.noura.platform.dto.location.StoreLocationDto;
import com.noura.platform.dto.location.StoreLocationRequest;
import com.noura.platform.dto.store.CreateStoreRequest;
import com.noura.platform.dto.store.StoreDto;
import com.noura.platform.dto.store.StoreRequest;
import com.noura.platform.dto.store.StoreResponse;
import com.noura.platform.dto.store.UpdateStoreStatusRequest;
import com.noura.platform.repository.MerchantRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.StoreTenantRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.StoreService;
import com.noura.platform.service.recovery.RecoveryGovernanceService;
import com.noura.platform.mapper.StoreMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Stores domain service covering admin, storefront, and contract-aware behavior.
 */
@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private static final String UNKNOWN_REGION = "GLOBAL";
    private static final String UNKNOWN_STATE = "STATE";
    private static final String UNKNOWN_ZIP = "00000";
    private static final String UNKNOWN_COUNTRY = "US";
    private static final int MAX_UNIQUE_ATTEMPTS = 12;

    private final StoreRepository storeRepository;
    private final MerchantRepository merchantRepository;
    private final UserAccountRepository userAccountRepository;
    private final StoreMapper storeMapper;
    private final RecoveryGovernanceService recoveryGovernanceService;
    private final StoreTenantRepository storeTenantRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "stores", key = "'list:' + #service + ':' + #openNow + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<StoreDto> listStores(String service, Boolean openNow, Pageable pageable) {
        Specification<Store> spec = Specification.<Store>where((root, query, cb) -> cb.isTrue(root.get("active")))
                .and(contractValiditySpec());
        if (service != null && !service.isBlank()) {
            StoreServiceType serviceType;
            try {
                serviceType = StoreServiceType.valueOf(service.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new NotFoundException("STORE_SERVICE_INVALID", "Unsupported store service filter");
            }
            spec = spec.and((root, query, cb) -> cb.isMember(serviceType, root.get("services")));
        }
        if (Boolean.TRUE.equals(openNow)) {
            LocalTime now = LocalTime.now();
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.lessThanOrEqualTo(root.get("openTime"), now),
                    cb.greaterThan(root.get("closeTime"), now)
            ));
        }
        return storeRepository.findAll(spec, pageable).map(entity -> enrich(entity, null, null));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "stores", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public StoreDto createStore(StoreRequest request) {
        Store store = new Store();
        toEntity(store, request);
        store.setStoreCode(resolveStoreCode(request.name(), null));
        store.setSlug(resolveStoreSlug(request.name(), null));
        store.setType(StoreType.MERCHANT);
        store.setStatus(request.active() ? StoreStatus.ACTIVE : StoreStatus.CLOSED);
        store.setActive(request.active());
        normalizeCoreStoreFields(store);
        ensureStoreCodeUniqueness(store, null);
        ensureStoreSlugUniqueness(store, null);
        validateTenantActivation(store, store.isActive());
        Store saved = saveWithIntegrity(() -> storeRepository.save(store));
        recoveryGovernanceService.captureVersion(
                "STORE",
                saved.getId().toString(),
                RecoveryActionType.CREATE,
                SecurityUtils.currentEmail(),
                "Store created.",
                Map.of()
        );
        return enrich(saved, null, null);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "stores", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public StoreDto updateStore(UUID storeId, StoreRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        toEntity(store, request);
        if (store.getStoreCode() == null || store.getStoreCode().isBlank()) {
            store.setStoreCode(resolveStoreCode(request.name(), storeId));
        } else {
            ensureStoreCodeUniqueness(store, storeId);
        }
        if (store.getSlug() == null || store.getSlug().isBlank()) {
            store.setSlug(resolveStoreSlug(request.name(), storeId));
        } else {
            ensureStoreSlugUniqueness(store, storeId);
        }
        store.setType(defaultStoreType(store.getType()));
        normalizeCoreStoreFields(store);
        store.setStatus(store.isActive() ? StoreStatus.ACTIVE : StoreStatus.CLOSED);
        validateTenantActivation(store, store.isActive());
        Store saved = saveWithIntegrity(() -> storeRepository.save(store));
        recoveryGovernanceService.captureVersion(
                "STORE",
                saved.getId().toString(),
                RecoveryActionType.UPDATE,
                SecurityUtils.currentEmail(),
                "Store updated.",
                Map.of()
        );
        return enrich(saved, null, null);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "stores", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteStore(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        store.setStatus(StoreStatus.CLOSED);
        store.setActive(false);
        storeRepository.save(store);
        recoveryGovernanceService.captureVersion(
                "STORE",
                store.getId().toString(),
                RecoveryActionType.UPDATE,
                SecurityUtils.currentEmail(),
                "Store deactivated.",
                Map.of("status", StoreStatus.CLOSED.name())
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "stores", key = "'nearest:' + #latitude + ':' + #longitude + ':' + #limit")
    public List<StoreDto> findNearest(BigDecimal latitude, BigDecimal longitude, int limit) {
        return storeRepository.findAll().stream()
                .sorted(Comparator.comparingDouble(store -> distanceKm(latitude, longitude, store)))
                .limit(limit)
                .map(store -> enrich(store, latitude, longitude))
                .toList();
    }

    @Override
    @Transactional
    public void setPreferredStore(UUID storeId) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        if (!storeRepository.existsById(storeId)) {
            throw new NotFoundException("STORE_NOT_FOUND", "Store not found");
        }
        user.setPreferredStoreId(storeId);
        userAccountRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public StoreLocationDto getStoreLocation(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        return toLocationDto(store);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "stores", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public StoreLocationDto updateStoreLocation(UUID storeId, StoreLocationRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        store.setAddressLine1(request.addressLine1());
        store.setCity(request.city());
        store.setState(request.state());
        store.setZipCode(request.zipCode());
        store.setCountry(request.country());
        store.setRegion(request.region());
        store.setLatitude(request.latitude());
        store.setLongitude(request.longitude());
        store.setServiceRadiusMeters(request.serviceRadiusMeters());
        store.setOpenTime(request.openTime());
        store.setCloseTime(request.closeTime());
        store.setActive(request.active());
        store.setStatus(request.active() ? StoreStatus.ACTIVE : StoreStatus.CLOSED);
        normalizeCoreStoreFields(store);
        validateTenantActivation(store, store.isActive());
        store.setServices(request.services());
        Store saved = storeRepository.save(store);
        recoveryGovernanceService.captureVersion(
                "STORE",
                saved.getId().toString(),
                RecoveryActionType.UPDATE,
                SecurityUtils.currentEmail(),
                "Store location updated.",
                Map.of()
        );
        return toLocationDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_STORES_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public Page<StoreResponse> listAdminStores(String search, UUID merchantId, StoreType type, StoreStatus status, Pageable pageable) {
        Specification<Store> spec = buildAdminStoreSpecification(search, merchantId, type, status);
        return storeRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "stores", allEntries = true)
    @PreAuthorize("hasAuthority('PERM_STORES_CREATE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public StoreResponse createAdminStore(CreateStoreRequest request) {
        if (!merchantRepository.existsById(request.merchantId())) {
            throw new NotFoundException("STORE_MERCHANT_NOT_FOUND", "Merchant not found");
        }

        Store store = new Store();
        store.setStoreCode(resolveStoreCode(request.storeCode(), null));
        store.setSlug(resolveStoreSlug(request.slug(), null));
        store.setMerchantId(request.merchantId());
        store.setName(request.name());
        store.setAddressLine1(request.addressLine1());
        store.setAddressLine2(request.addressLine2());
        store.setCity(request.city());
        store.setCountryCode(normalizeCountryCode(request.countryCode()));
        store.setContactEmail(normalizeEmail(request.contactEmail()));
        store.setContactPhone(trim(request.contactPhone()));
        store.setType(request.type());
        store.setStatus(request.status());
        store.setActive(request.status() == StoreStatus.ACTIVE);
        normalizeCoreStoreFields(store);
        store.setServices(new HashSet<>());

        ensureStoreCodeUniqueness(store, null);
        ensureStoreSlugUniqueness(store, null);
        validateMerchantStoreConstraints(store);
        validateTenantActivation(store, store.isActive());
        Store saved = saveWithIntegrity(() -> storeRepository.save(store));
        recoveryGovernanceService.captureVersion(
                "STORE",
                saved.getId().toString(),
                RecoveryActionType.CREATE,
                SecurityUtils.currentEmail(),
                "Store created.",
                Map.of(
                        "merchantId", saved.getMerchantId().toString(),
                        "storeCode", saved.getStoreCode(),
                        "slug", saved.getSlug()
                )
        );
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_STORES_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public StoreResponse getAdminStore(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        return toResponse(store);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "stores", allEntries = true)
    @PreAuthorize("hasAuthority('PERM_STORES_UPDATE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public StoreResponse updateStoreStatus(UUID storeId, UpdateStoreStatusRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        store.setStatus(request.status());
        store.setActive(request.status() == StoreStatus.ACTIVE);
        validateTenantActivation(store, store.isActive());
        Store saved = storeRepository.save(store);
        recoveryGovernanceService.captureVersion(
                "STORE",
                saved.getId().toString(),
                RecoveryActionType.UPDATE,
                SecurityUtils.currentEmail(),
                "Store status updated.",
                Map.of("status", saved.getStatus().name())
        );
        return toResponse(saved);
    }

    private Specification<Store> buildAdminStoreSpecification(String search, UUID merchantId, StoreType type, StoreStatus status) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (merchantId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("merchantId"), merchantId));
            }
            if (type != null) {
                predicate = cb.and(predicate, cb.equal(root.get("type"), type));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (search == null || search.isBlank()) {
                return predicate;
            }

            String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            Predicate storeName = cb.like(cb.lower(root.get("name")), like);
            Predicate storeCode = cb.like(cb.lower(root.get("storeCode")), like);
            Predicate slug = cb.like(cb.lower(root.get("slug")), like);

            Subquery<UUID> merchantIds = query.subquery(UUID.class);
            Root<Merchant> merchantRoot = merchantIds.from(Merchant.class);
            merchantIds.select(merchantRoot.get("id")).where(cb.or(
                    cb.like(cb.lower(merchantRoot.get("legalName")), like),
                    cb.like(cb.lower(merchantRoot.get("displayName")), like)
            ));
            Predicate merchantSearch = root.get("merchantId").in(merchantIds);

            return cb.and(predicate, cb.or(storeName, storeCode, slug, merchantSearch));
        };
    }

    private void fillLegacyDefaults(Store store) {
        if (store.getState() == null || store.getState().isBlank()) {
            store.setState(UNKNOWN_STATE);
        }
        if (store.getZipCode() == null || store.getZipCode().isBlank()) {
            store.setZipCode(UNKNOWN_ZIP);
        }
        if (store.getCountry() == null || store.getCountry().isBlank()) {
            store.setCountry(UNKNOWN_COUNTRY);
        }
        if (store.getRegion() == null || store.getRegion().isBlank()) {
            store.setRegion(UNKNOWN_REGION);
        }
        if (store.getShippingFee() == null) {
            store.setShippingFee(BigDecimal.ZERO);
        }
        if (store.getFreeShippingThreshold() == null) {
            store.setFreeShippingThreshold(BigDecimal.ZERO);
        }
        if (store.getType() == null) {
            store.setType(StoreType.MERCHANT);
        }
        if (store.getServices() == null) {
            store.setServices(new HashSet<>());
        }
        if (store.getStatus() == null) {
            store.setStatus(StoreStatus.CLOSED);
        }
        if (store.getOpenTime() == null) {
            store.setOpenTime(LocalTime.MIDNIGHT);
        }
        if (store.getCloseTime() == null) {
            store.setCloseTime(LocalTime.of(23, 59));
        }
        store.setActive(store.getStatus() == StoreStatus.ACTIVE);
    }

    private void normalizeCoreStoreFields(Store store) {
        store.setName(trim(store.getName()));
        store.setAddressLine1(trim(store.getAddressLine1()));
        store.setAddressLine2(trim(store.getAddressLine2()));
        store.setCity(trim(store.getCity()));
        store.setCountry(trim(store.getCountry()));
        if (store.getCountry() == null || store.getCountry().isBlank()) {
            store.setCountry(UNKNOWN_COUNTRY);
        }
        store.setRegion(trim(store.getRegion()) != null ? trim(store.getRegion()) : UNKNOWN_REGION);
        if (store.getLatitude() == null) {
            store.setLatitude(BigDecimal.ZERO);
        }
        if (store.getLongitude() == null) {
            store.setLongitude(BigDecimal.ZERO);
        }
        if (store.getServiceRadiusMeters() == null) {
            store.setServiceRadiusMeters(0);
        }
        if (store.getOpenTime() == null) {
            store.setOpenTime(LocalTime.MIDNIGHT);
        }
        if (store.getCloseTime() == null) {
            store.setCloseTime(LocalTime.of(23, 59));
        }
        if (store.getState() == null || store.getState().isBlank()) {
            store.setState(UNKNOWN_STATE);
        }
        if (store.getZipCode() == null || store.getZipCode().isBlank()) {
            store.setZipCode(UNKNOWN_ZIP);
        }
        if (store.getRegion() == null || store.getRegion().isBlank()) {
            store.setRegion(UNKNOWN_REGION);
        }
        if (store.getCountryCode() == null || store.getCountryCode().isBlank()) {
            store.setCountryCode(null);
        } else {
            store.setCountryCode(store.getCountryCode().trim().toUpperCase(Locale.ROOT));
        }
        if (store.getContactEmail() != null) {
            String normalizedEmail = normalizeEmail(store.getContactEmail());
            store.setContactEmail(normalizedEmail);
        }
        if (store.getContactPhone() != null) {
            store.setContactPhone(trim(store.getContactPhone()));
        }
        fillLegacyDefaults(store);
    }

    private void validateMerchantStoreConstraints(Store store) {
        if (store.getMerchantId() == null) {
            throw new BadRequestException("STORE_MERCHANT_REQUIRED", "Merchant id is required");
        }
        if (store.getStatus() == null) {
            store.setStatus(StoreStatus.ACTIVE);
        }
        if (store.getType() == null) {
            store.setType(StoreType.MERCHANT);
        }
        if (store.getCity() == null || store.getCity().isBlank()) {
            throw new BadRequestException("STORE_CITY_REQUIRED", "City is required");
        }
        if (store.getAddressLine1() == null || store.getAddressLine1().isBlank()) {
            throw new BadRequestException("STORE_ADDRESS_REQUIRED", "Address line 1 is required");
        }
        if (store.getStoreCode() == null || store.getStoreCode().isBlank()) {
            throw new BadRequestException("STORE_CODE_REQUIRED", "Store code is required");
        }
        if (store.getSlug() == null || store.getSlug().isBlank()) {
            throw new BadRequestException("STORE_SLUG_REQUIRED", "Store slug is required");
        }
    }

    private Store toEntity(Store entity, StoreRequest request) {
        entity.setName(request.name());
        entity.setAddressLine1(request.addressLine1());
        entity.setCity(request.city());
        entity.setState(request.state());
        entity.setZipCode(request.zipCode());
        entity.setCountry(request.country());
        entity.setRegion(request.region());
        entity.setLatitude(request.latitude());
        entity.setLongitude(request.longitude());
        entity.setServiceRadiusMeters(request.serviceRadiusMeters());
        entity.setOpenTime(request.openTime());
        entity.setCloseTime(request.closeTime());
        entity.setActive(request.active());
        entity.setServices(request.services());
        entity.setShippingFee(request.shippingFee());
        entity.setFreeShippingThreshold(request.freeShippingThreshold());
        return entity;
    }

    private StoreDto enrich(Store store, BigDecimal latitude, BigDecimal longitude) {
        StoreDto base = storeMapper.toDto(store);
        double distance = (latitude == null || longitude == null) ? 0D : distanceKm(latitude, longitude, store);
        LocalTime now = LocalTime.now();
        boolean openNow = !now.isBefore(store.getOpenTime()) && now.isBefore(store.getCloseTime());
        return new StoreDto(
                base.id(),
                base.name(),
                base.addressLine1(),
                base.city(),
                base.state(),
                base.zipCode(),
                base.country(),
                base.region(),
                base.latitude(),
                base.longitude(),
                base.serviceRadiusMeters(),
                base.openTime(),
                base.closeTime(),
                base.active(),
                base.services(),
                base.shippingFee(),
                base.freeShippingThreshold(),
                distance,
                openNow
        );
    }

    private StoreResponse toResponse(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getStoreCode(),
                store.getMerchantId(),
                store.getName(),
                store.getSlug(),
                store.getType(),
                store.getStatus(),
                store.getContactEmail(),
                store.getContactPhone(),
                store.getCountryCode(),
                store.getCity(),
                store.getAddressLine1(),
                store.getAddressLine2(),
                store.getCreatedAt(),
                store.getUpdatedAt(),
                store.getCreatedBy(),
                store.getUpdatedBy()
        );
    }

    private void ensureStoreCodeUniqueness(Store store, UUID currentStoreId) {
        String normalized = normalizeStoreCode(store.getStoreCode());
        if (normalized == null || normalized.isBlank()) {
            throw new BadRequestException("STORE_CODE_REQUIRED", "Store code is required");
        }
        if (currentStoreId == null) {
            if (!storeRepository.existsByStoreCodeIgnoreCase(normalized)) {
                store.setStoreCode(normalized);
                return;
            }
        } else if (!storeRepository.existsByStoreCodeIgnoreCaseAndIdNot(normalized, currentStoreId)) {
            store.setStoreCode(normalized);
            return;
        }
        store.setStoreCode(resolveStoreCode(normalized, currentStoreId));
    }

    private void ensureStoreSlugUniqueness(Store store, UUID currentStoreId) {
        String normalized = normalizeStoreSlug(store.getSlug());
        if (normalized == null || normalized.isBlank()) {
            throw new BadRequestException("STORE_SLUG_REQUIRED", "Store slug is required");
        }
        if (currentStoreId == null) {
            if (!storeRepository.existsBySlugIgnoreCase(normalized)) {
                store.setSlug(normalized);
                return;
            }
        } else if (!storeRepository.existsBySlugIgnoreCaseAndIdNot(normalized, currentStoreId)) {
            store.setSlug(normalized);
            return;
        }
        store.setSlug(resolveStoreSlug(normalized, currentStoreId));
    }

    private String resolveStoreCode(String preferred, UUID currentStoreId) {
        String seed = normalizeStoreCode(preferred);
        if (seed == null || seed.isBlank()) {
            seed = "STORE-" + randomSuffix();
        }
        if (seed.length() > 80) {
            seed = seed.substring(0, 80);
        }
        String candidateBase = seed;
        if (currentStoreId == null) {
            if (!storeRepository.existsByStoreCodeIgnoreCase(candidateBase)) {
                return candidateBase;
            }
        } else if (!storeRepository.existsByStoreCodeIgnoreCaseAndIdNot(candidateBase, currentStoreId)) {
            return candidateBase;
        }

        for (int i = 1; i <= MAX_UNIQUE_ATTEMPTS; i++) {
            String suffix = "-" + i;
            int maxBaseLen = Math.max(1, 80 - suffix.length());
            String base = candidateBase.substring(0, Math.min(candidateBase.length(), maxBaseLen));
            String candidate = base + suffix;
            if (currentStoreId == null) {
                if (!storeRepository.existsByStoreCodeIgnoreCase(candidate)) {
                    return candidate;
                }
            } else if (!storeRepository.existsByStoreCodeIgnoreCaseAndIdNot(candidate, currentStoreId)) {
                return candidate;
            }
        }
        throw new BadRequestException("STORE_CODE_DUPLICATE", "Could not resolve unique store code");
    }

    private String resolveStoreSlug(String preferred, UUID currentStoreId) {
        String seed = normalizeStoreSlug(preferred);
        if (seed == null || seed.isBlank()) {
            seed = "store";
        }
        if (seed.length() > 255) {
            seed = seed.substring(0, 255);
        }
        String candidateBase = seed;
        if (currentStoreId == null) {
            if (!storeRepository.existsBySlugIgnoreCase(candidateBase)) {
                return candidateBase;
            }
        } else if (!storeRepository.existsBySlugIgnoreCaseAndIdNot(candidateBase, currentStoreId)) {
            return candidateBase;
        }

        for (int i = 1; i <= MAX_UNIQUE_ATTEMPTS; i++) {
            String suffix = "-" + i;
            int maxBaseLen = Math.max(1, 255 - suffix.length());
            String base = candidateBase.substring(0, Math.min(candidateBase.length(), maxBaseLen));
            String candidate = base + suffix;
            if (currentStoreId == null) {
                if (!storeRepository.existsBySlugIgnoreCase(candidate)) {
                    return candidate;
                }
            } else if (!storeRepository.existsBySlugIgnoreCaseAndIdNot(candidate, currentStoreId)) {
                return candidate;
            }
        }
        throw new BadRequestException("STORE_SLUG_DUPLICATE", "Could not resolve unique store slug");
    }

    private String normalizeStoreCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeStoreSlug(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeCountryCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }

    private Store saveWithIntegrity(Supplier<Store> saver) {
        try {
            return saver.get();
        } catch (DataIntegrityViolationException ex) {
            String message = ex.getMessage();
            if (message != null) {
                if (message.contains("store_code") || message.contains("stores_store_code")) {
                    throw new BadRequestException("STORE_CODE_DUPLICATE", "Store code already exists");
                }
                if (message.contains("slug") || message.contains("stores_slug")) {
                    throw new BadRequestException("STORE_SLUG_DUPLICATE", "Store slug already exists");
                }
            }
            throw ex;
        }
    }

    private double distanceKm(BigDecimal latitude, BigDecimal longitude, Store store) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(store.getLatitude().doubleValue() - latitude.doubleValue());
        double dLng = Math.toRadians(store.getLongitude().doubleValue() - longitude.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude.doubleValue()))
                * Math.cos(Math.toRadians(store.getLatitude().doubleValue()))
                * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private StoreLocationDto toLocationDto(Store store) {
        return new StoreLocationDto(
                store.getId(),
                store.getName(),
                store.getAddressLine1(),
                store.getCity(),
                store.getState(),
                store.getZipCode(),
                store.getCountry(),
                store.getRegion(),
                store.getLatitude(),
                store.getLongitude(),
                store.getServiceRadiusMeters(),
                store.getOpenTime(),
                store.getCloseTime(),
                store.isActive(),
                store.getServices()
        );
    }

    private Specification<Store> contractValiditySpec() {
        return (root, query, cb) -> {
            Subquery<Long> tenantExists = query.subquery(Long.class);
            Root<StoreTenant> tenantRoot = tenantExists.from(StoreTenant.class);
            tenantExists.select(cb.literal(1L));
            tenantExists.where(cb.equal(tenantRoot.get("store").get("id"), root.get("id")));

            Subquery<Long> validTenant = query.subquery(Long.class);
            Root<StoreTenant> validTenantRoot = validTenant.from(StoreTenant.class);
            var contractJoin = validTenantRoot.join("contract");
            LocalDate today = LocalDate.now();
            validTenant.select(cb.literal(1L));
            validTenant.where(
                    cb.equal(validTenantRoot.get("store").get("id"), root.get("id")),
                    cb.equal(validTenantRoot.get("status"), StoreTenantStatus.ACTIVE),
                    cb.equal(contractJoin.get("status"), MerchantContractStatus.APPROVED),
                    cb.lessThanOrEqualTo(contractJoin.get("startDate"), today),
                    cb.or(
                            cb.isNull(contractJoin.get("endDate")),
                            cb.greaterThanOrEqualTo(contractJoin.get("endDate"), today)
                    )
            );
            return cb.or(cb.not(cb.exists(tenantExists)), cb.exists(validTenant));
        };
    }

    private StoreType defaultStoreType(StoreType type) {
        return type == null ? StoreType.MERCHANT : type;
    }

    private void validateTenantActivation(Store store, boolean requestedActive) {
        if (!requestedActive || store.getId() == null) {
            return;
        }
        StoreTenant tenant = storeTenantRepository.findByStoreId(store.getId()).orElse(null);
        if (tenant == null) {
            return;
        }
        if (tenant.getStatus() != StoreTenantStatus.ACTIVE) {
            throw new BadRequestException(
                    "STORE_CONTRACT_INACTIVE",
                    "Store cannot be activated until the tenant registration is ACTIVE."
            );
        }
        if (tenant.getContract() == null || tenant.getContract().getStatus() != MerchantContractStatus.APPROVED) {
            throw new BadRequestException(
                    "STORE_CONTRACT_NOT_APPROVED",
                    "Store cannot be activated until the contract is APPROVED."
            );
        }
        LocalDate today = LocalDate.now();
        LocalDate start = tenant.getContract().getStartDate();
        LocalDate end = tenant.getContract().getEndDate();
        if (start != null && today.isBefore(start)) {
            throw new BadRequestException(
                    "STORE_CONTRACT_NOT_STARTED",
                    "Store cannot be activated before contract start date."
            );
        }
        if (end != null && today.isAfter(end)) {
            throw new BadRequestException(
                    "STORE_CONTRACT_EXPIRED",
                    "Store cannot be activated after contract end date."
            );
        }
    }
}
