package com.noura.platform.service.impl;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.StoreServiceType;
import com.noura.platform.dto.store.StoreDto;
import com.noura.platform.dto.store.StoreRequest;
import com.noura.platform.mapper.StoreMapper;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final UserAccountRepository userAccountRepository;
    private final StoreMapper storeMapper;

    /**
     * Lists stores.
     *
     * @param service The service value.
     * @param openNow The open now value.
     * @param pageable The pagination configuration.
     * @return A paginated result set.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "stores", key = "'list:' + #service + ':' + #openNow + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<StoreDto> listStores(String service, Boolean openNow, Pageable pageable) {
        Specification<Store> spec = Specification.where((root, query, cb) -> cb.isTrue(root.get("active")));
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

    /**
     * Creates store.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "stores", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public StoreDto createStore(StoreRequest request) {
        return enrich(storeRepository.save(toEntity(new Store(), request)), null, null);
    }

    /**
     * Updates store.
     *
     * @param storeId The store id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "stores", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public StoreDto updateStore(UUID storeId, StoreRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        return enrich(storeRepository.save(toEntity(store, request)), null, null);
    }

    /**
     * Deletes store.
     *
     * @param storeId The store id used to locate the target record.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "stores", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteStore(UUID storeId) {
        storeRepository.deleteById(storeId);
    }

    /**
     * Finds nearest.
     *
     * @param latitude The latitude value.
     * @param longitude The longitude value.
     * @param limit The limit value.
     * @return A list of matching items.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "stores", key = "'nearest:' + #latitude + ':' + #longitude + ':' + #limit")
    public List<StoreDto> findNearest(BigDecimal latitude, BigDecimal longitude, int limit) {
        return storeRepository.findAll()
                .stream()
                .sorted(Comparator.comparingDouble(store -> distanceKm(latitude, longitude, store)))
                .limit(limit)
                .map(store -> enrich(store, latitude, longitude))
                .toList();
    }

    /**
     * Sets preferred store.
     *
     * @param storeId The store id used to locate the target record.
     */
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

    /**
     * Executes to entity.
     *
     * @param entity The source object to transform.
     * @param request The request payload for this operation.
     * @return The result of to entity.
     */
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
        entity.setOpenTime(request.openTime());
        entity.setCloseTime(request.closeTime());
        entity.setActive(request.active());
        entity.setServices(request.services());
        entity.setShippingFee(request.shippingFee());
        entity.setFreeShippingThreshold(request.freeShippingThreshold());
        return entity;
    }

    /**
     * Executes enrich.
     *
     * @param store The store value.
     * @param latitude The latitude value.
     * @param longitude The longitude value.
     * @return The mapped DTO representation.
     */
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

    /**
     * Executes distance km.
     *
     * @param latitude The latitude value.
     * @param longitude The longitude value.
     * @param store The store value.
     * @return The result of distance km.
     */
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
}
