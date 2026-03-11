package com.noura.platform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.common.exception.UnauthorizedException;
import com.noura.platform.domain.entity.ServiceArea;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.entity.UserLocation;
import com.noura.platform.domain.enums.LocationSource;
import com.noura.platform.domain.enums.ServiceAreaStatus;
import com.noura.platform.domain.enums.ServiceAreaType;
import com.noura.platform.domain.enums.StoreServiceType;
import com.noura.platform.dto.location.GeocodeResultDto;
import com.noura.platform.dto.location.LocationResolveDto;
import com.noura.platform.dto.location.LocationResolveRequest;
import com.noura.platform.dto.location.NearbyStoreDto;
import com.noura.platform.dto.location.ReverseGeocodeRequest;
import com.noura.platform.dto.location.ServiceAreaValidationRequest;
import com.noura.platform.dto.location.ServiceEligibilityDto;
import com.noura.platform.location.util.GeoJsonUtils;
import com.noura.platform.location.util.GeoUtils;
import com.noura.platform.repository.ServiceAreaRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.repository.UserLocationRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.LocationGeocodingService;
import com.noura.platform.service.LocationIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationIntelligenceServiceImpl implements LocationIntelligenceService {
    private static final Logger log = LoggerFactory.getLogger(LocationIntelligenceServiceImpl.class);

    private final LocationGeocodingService geocodingService;
    private final ServiceAreaRepository serviceAreaRepository;
    private final StoreRepository storeRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserLocationRepository userLocationRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.location.privacy.persist-user-location:true}")
    private boolean persistUserLocationEnabled;

    @Value("${app.location.privacy.persist-precision-decimals:5}")
    private int persistPrecisionDecimals;

    @Value("${app.location.privacy.max-accuracy-meters:200}")
    private int maxTrustedAccuracyMeters;

    @Override
    @Transactional(readOnly = true)
    public ServiceEligibilityDto validate(ServiceAreaValidationRequest request) {
        StoreServiceType serviceType = request.serviceType() == null ? StoreServiceType.DELIVERY : request.serviceType();
        Instant at = request.at() == null ? Instant.now() : request.at();
        LocalTime checkTime = LocalDateTime.ofInstant(at, ZoneId.systemDefault()).toLocalTime();

        List<ServiceArea> activeAreas = new ArrayList<>(serviceAreaRepository.findByStatus(ServiceAreaStatus.ACTIVE));
        boolean hasConfiguredAreas = !activeAreas.isEmpty();

        ServiceArea matchedArea = selectMatchingArea(activeAreas, request.latitude(), request.longitude());
        boolean insideServiceArea = !hasConfiguredAreas || matchedArea != null;
        AreaRules areaRules = parseAreaRules(matchedArea);

        if (matchedArea != null && areaRules.disallows(serviceType)) {
            return buildEligibility(
                    false,
                    serviceType,
                    matchedArea,
                    null,
                    null,
                    true,
                    false,
                    serviceType == StoreServiceType.PICKUP ? "DELIVERY_ONLY_AREA" : "PICKUP_ONLY_AREA"
            );
        }

        Integer effectiveMaxDistance = effectiveMaxDistance(request.maxDistanceMeters(), areaRules.maxDistanceMeters());
        List<CandidateStore> scopedCandidates = candidateStores(matchedArea, serviceType, request.latitude(), request.longitude(), checkTime);
        List<CandidateStore> rangeEligible = scopedCandidates.stream()
                .filter(candidate -> effectiveMaxDistance == null || candidate.distanceMeters() <= effectiveMaxDistance)
                .toList();

        CandidateStore fallbackNearest = selectCandidate(
                candidateStores(null, serviceType, request.latitude(), request.longitude(), checkTime),
                null
        );

        if (!insideServiceArea) {
            return buildEligibility(
                    false,
                    serviceType,
                    null,
                    fallbackNearest,
                    fallbackNearest == null ? null : fallbackNearest.distanceMeters(),
                    false,
                    fallbackNearest != null && fallbackNearest.openNow(),
                    "SERVICE_AREA_MISS"
            );
        }

        if (scopedCandidates.isEmpty()) {
            return buildEligibility(false, serviceType, matchedArea, null, null, true, false, "NO_STORE_AVAILABLE");
        }

        if (rangeEligible.isEmpty()) {
            CandidateStore nearest = selectCandidate(scopedCandidates, areaRules.defaultStoreId());
            return buildEligibility(
                    false,
                    serviceType,
                    matchedArea,
                    nearest,
                    nearest == null ? null : nearest.distanceMeters(),
                    true,
                    nearest != null && nearest.openNow(),
                    "OUT_OF_RANGE"
            );
        }

        CandidateStore selected = selectCandidate(rangeEligible, areaRules.defaultStoreId());
        if (selected == null) {
            return buildEligibility(false, serviceType, matchedArea, null, null, true, false, "NO_STORE_AVAILABLE");
        }

        String reason;
        boolean available;
        if (!selected.openNow()) {
            available = false;
            reason = "STORE_CLOSED";
        } else {
            available = true;
            reason = hasConfiguredAreas ? "AVAILABLE" : "AVAILABLE_NO_SERVICE_AREAS";
        }

        ServiceEligibilityDto eligibility = buildEligibility(
                available,
                serviceType,
                matchedArea,
                selected,
                selected.distanceMeters(),
                true,
                selected.openNow(),
                reason
        );
        log.info(
                "delivery_eligibility_checked serviceType={} available={} areaId={} storeId={} reason={}",
                serviceType,
                available,
                eligibility.matchedServiceAreaId(),
                eligibility.matchedStoreId(),
                eligibility.eligibilityReason()
        );
        return eligibility;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NearbyStoreDto> nearbyStores(BigDecimal latitude,
                                            BigDecimal longitude,
                                            StoreServiceType serviceType,
                                            Boolean openNow,
                                            Integer limit,
                                            Integer maxDistanceMeters) {
        if (latitude == null || longitude == null) {
            throw new BadRequestException("GEO_COORDINATES_REQUIRED", "Latitude and longitude are required.");
        }
        int resolvedLimit = limit == null ? 10 : Math.min(Math.max(limit, 1), 25);

        LocalTime now = LocalTime.now();

        return storeRepository.findAll()
                .stream()
                .filter(Store::isActive)
                .filter(store -> serviceType == null || store.getServices().contains(serviceType))
                .map(store -> toNearbyDto(store, latitude, longitude, now))
                .filter(dto -> serviceType != StoreServiceType.DELIVERY
                        || dto.serviceRadiusMeters() == null
                        || dto.serviceRadiusMeters() <= 0
                        || dto.distanceMeters() <= dto.serviceRadiusMeters())
                .filter(dto -> maxDistanceMeters == null || dto.distanceMeters() <= maxDistanceMeters)
                .filter(dto -> !Boolean.TRUE.equals(openNow) || dto.openNow())
                .sorted(Comparator.comparingLong(NearbyStoreDto::distanceMeters))
                .limit(resolvedLimit)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public LocationResolveDto resolve(LocationResolveRequest request, String actor) {
        if (Boolean.TRUE.equals(request.persist()) && !request.consentGiven()) {
            throw new BadRequestException("LOCATION_CONSENT_REQUIRED", "Consent is required to persist a location capture.");
        }
        if (Boolean.TRUE.equals(request.persist()) && (actor == null || actor.isBlank())) {
            throw new UnauthorizedException("AUTH_REQUIRED", "Authentication required to persist location.");
        }

        GeocodeResultDto geocode = null;
        try {
            geocode = geocodingService.reverseGeocode(new ReverseGeocodeRequest(request.latitude(), request.longitude(), null));
        } catch (Exception ex) {
            // Provider outages should not prevent coordinate-based eligibility checks from proceeding.
            log.warn("Reverse geocode unavailable during resolve: {}", ex.getMessage());
        }
        ServiceEligibilityDto eligibility = validate(new ServiceAreaValidationRequest(
                request.latitude(),
                request.longitude(),
                request.serviceType() == null ? StoreServiceType.DELIVERY : request.serviceType(),
                Instant.now(),
                null
        ));

        UUID locationId = null;
        if (persistUserLocationEnabled && request.persist()) {
            String email = actor == null ? SecurityUtils.currentEmail() : actor;
            UserAccount user = userAccountRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Authenticated user not found"));
            UserLocation capture = new UserLocation();
            capture.setUser(user);
            capture.setLatitude(GeoUtils.roundCoordinate(request.latitude(), persistPrecisionDecimals));
            capture.setLongitude(GeoUtils.roundCoordinate(request.longitude(), persistPrecisionDecimals));
            capture.setAccuracyMeters(request.accuracyMeters());
            capture.setSource(request.source() == null ? LocationSource.BROWSER : request.source());
            capture.setConsentGiven(request.consentGiven());
            capture.setPurpose(request.purpose());
            capture.setCapturedAt(Instant.now());
            capture.setFormattedAddress(geocode == null ? null : geocode.formattedAddress());
            capture.setCountry(geocode == null ? null : geocode.country());
            capture.setRegion(geocode == null ? null : geocode.region());
            capture.setCity(geocode == null ? null : geocode.city());
            capture.setDistrict(geocode == null ? null : geocode.district());
            capture.setPostalCode(geocode == null ? null : geocode.postalCode());
            capture.setPlaceId(geocode == null ? null : geocode.placeId());

            boolean verified = isTrustedAccuracy(request.accuracyMeters());
            Optional<UserLocation> last = userLocationRepository.findFirstByUserOrderByCapturedAtDesc(user);
            if (last.isPresent()) {
                UserLocation prev = last.get();
                long distance = GeoUtils.haversineMeters(capture.getLatitude(), capture.getLongitude(), prev.getLatitude(), prev.getLongitude());
                long seconds = Math.abs(capture.getCapturedAt().getEpochSecond() - prev.getCapturedAt().getEpochSecond());
                if (seconds <= 300 && distance >= 500_000) {
                    // Fraud/validation signal: improbable jump in a short time window.
                    verified = false;
                    log.warn("Suspicious location jump detected for user {}: {}m in {}s", email, distance, seconds);
                }
            }
            capture.setVerified(verified);

            UserLocation saved = userLocationRepository.save(capture);
            locationId = saved.getId();
        }

        if (locationId != null) {
            log.info("location_captured locationId={} source={} verified={}", locationId, request.source(), eligibility.eligibilityReason());
        }
        return new LocationResolveDto(locationId, geocode, eligibility);
    }

    private boolean isTrustedAccuracy(Integer accuracyMeters) {
        if (accuracyMeters == null) return true;
        return accuracyMeters > 0 && accuracyMeters <= maxTrustedAccuracyMeters;
    }

    private boolean isOpenAt(Store store, LocalTime time) {
        if (store == null || time == null) return false;
        return !time.isBefore(store.getOpenTime()) && time.isBefore(store.getCloseTime());
    }

    private NearbyStoreDto toNearbyDto(Store store, BigDecimal latitude, BigDecimal longitude, LocalTime now) {
        long distanceMeters = GeoUtils.haversineMeters(latitude, longitude, store.getLatitude(), store.getLongitude());
        boolean openNow = isOpenAt(store, now);
        return new NearbyStoreDto(
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
                store.getServices(),
                distanceMeters,
                openNow
        );
    }

    /**
     * Deterministically selects the most specific matching area.
     */
    private ServiceArea selectMatchingArea(List<ServiceArea> activeAreas, BigDecimal latitude, BigDecimal longitude) {
        if (activeAreas == null || activeAreas.isEmpty()) return null;

        // Only call reverse-geocode when required for CITY/DISTRICT matching.
        boolean needsAdminAreas = activeAreas.stream().anyMatch(area ->
                area.getType() == ServiceAreaType.CITY || area.getType() == ServiceAreaType.DISTRICT
        );
        GeocodeResultDto geocode = null;
        if (needsAdminAreas) {
            try {
                geocode = geocodingService.reverseGeocode(new ReverseGeocodeRequest(latitude, longitude, null));
            } catch (Exception ex) {
                // Degrade gracefully: polygon/radius evaluation can still work without geocoding.
                log.warn("Reverse geocode unavailable for CITY/DISTRICT matching: {}", ex.getMessage());
            }
        }

        final GeocodeResultDto finalGeocode = geocode;
        return activeAreas.stream()
                .filter(area -> contains(area, latitude, longitude, finalGeocode))
                .sorted(Comparator
                        .comparingInt((ServiceArea area) -> specificityRank(area.getType()))
                        .thenComparing(area -> safe(area.getName()))
                        .thenComparing(area -> area.getId() == null ? "" : area.getId().toString())
                )
                .findFirst()
                .orElse(null);
    }

    private boolean contains(ServiceArea area, BigDecimal latitude, BigDecimal longitude, GeocodeResultDto geocode) {
        if (area == null || latitude == null || longitude == null) return false;
        if (area.getType() == null) return false;

        return switch (area.getType()) {
            case RADIUS -> containsRadius(area, latitude, longitude);
            case POLYGON -> containsPolygon(area, latitude, longitude);
            case CITY -> containsCity(area, geocode);
            case DISTRICT -> containsDistrict(area, geocode);
        };
    }

    private boolean containsRadius(ServiceArea area, BigDecimal latitude, BigDecimal longitude) {
        if (area.getCenterLatitude() == null || area.getCenterLongitude() == null || area.getRadiusMeters() == null) {
            return false;
        }
        long distance = GeoUtils.haversineMeters(latitude, longitude, area.getCenterLatitude(), area.getCenterLongitude());
        return distance <= Math.max(0, area.getRadiusMeters());
    }

    private boolean containsPolygon(ServiceArea area, BigDecimal latitude, BigDecimal longitude) {
        List<GeoUtils.Point> ring = GeoJsonUtils.extractOuterRing(area.getPolygonGeoJson());
        if (ring.isEmpty()) return false;
        return GeoUtils.pointInPolygon(latitude.doubleValue(), longitude.doubleValue(), ring);
    }

    private boolean containsCity(ServiceArea area, GeocodeResultDto geocode) {
        String city = geocode == null ? null : safe(geocode.city());
        if (city == null) return false;
        return city.equalsIgnoreCase(safe(area.getName()));
    }

    private boolean containsDistrict(ServiceArea area, GeocodeResultDto geocode) {
        String district = geocode == null ? null : safe(geocode.district());
        if (district == null) return false;
        return district.equalsIgnoreCase(safe(area.getName()));
    }

    private int specificityRank(ServiceAreaType type) {
        if (type == null) return 100;
        return switch (type) {
            case DISTRICT -> 1;
            case CITY -> 2;
            case POLYGON -> 3;
            case RADIUS -> 4;
        };
    }

    private List<CandidateStore> candidateStores(
            ServiceArea matchedArea,
            StoreServiceType serviceType,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalTime checkTime
    ) {
        List<Store> allStores = storeRepository.findAll();
        List<Store> scoped = matchedArea != null && matchedArea.getStores() != null && !matchedArea.getStores().isEmpty()
                ? matchedArea.getStores().stream().toList()
                : allStores;

        return scoped.stream()
                .filter(Store::isActive)
                .filter(store -> serviceType == null || store.getServices().contains(serviceType))
                .map(store -> new CandidateStore(
                        store,
                        GeoUtils.haversineMeters(latitude, longitude, store.getLatitude(), store.getLongitude()),
                        isOpenAt(store, checkTime)
                ))
                .filter(candidate -> serviceType != StoreServiceType.DELIVERY
                        || candidate.store().getServiceRadiusMeters() == null
                        || candidate.store().getServiceRadiusMeters() <= 0
                        || candidate.distanceMeters() <= candidate.store().getServiceRadiusMeters())
                .toList();
    }

    private String safe(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    private CandidateStore selectCandidate(List<CandidateStore> candidates, UUID defaultStoreId) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        if (defaultStoreId != null) {
            Optional<CandidateStore> defaultOpen = candidates.stream()
                    .filter(candidate -> defaultStoreId.equals(candidate.store().getId()))
                    .filter(CandidateStore::openNow)
                    .findFirst();
            if (defaultOpen.isPresent()) {
                return defaultOpen.get();
            }
        }

        Optional<CandidateStore> nearestOpen = candidates.stream()
                .filter(CandidateStore::openNow)
                .min(Comparator.comparingLong(CandidateStore::distanceMeters));
        if (nearestOpen.isPresent()) {
            return nearestOpen.get();
        }

        if (defaultStoreId != null) {
            Optional<CandidateStore> defaultAny = candidates.stream()
                    .filter(candidate -> defaultStoreId.equals(candidate.store().getId()))
                    .findFirst();
            if (defaultAny.isPresent()) {
                return defaultAny.get();
            }
        }

        return candidates.stream()
                .min(Comparator.comparingLong(CandidateStore::distanceMeters))
                .orElse(null);
    }

    private Integer effectiveMaxDistance(Integer requestDistance, Integer rulesDistance) {
        if (requestDistance == null) return rulesDistance;
        if (rulesDistance == null) return requestDistance;
        return Math.min(requestDistance, rulesDistance);
    }

    private ServiceEligibilityDto buildEligibility(
            boolean serviceAvailable,
            StoreServiceType serviceType,
            ServiceArea matchedArea,
            CandidateStore candidate,
            Long distanceMeters,
            boolean insideServiceArea,
            boolean storeOpenNow,
            String reason
    ) {
        return new ServiceEligibilityDto(
                serviceAvailable,
                serviceType,
                matchedArea == null ? null : matchedArea.getId(),
                candidate == null ? null : candidate.store().getId(),
                distanceMeters,
                insideServiceArea,
                storeOpenNow,
                reason
        );
    }

    private AreaRules parseAreaRules(ServiceArea area) {
        if (area == null || area.getRulesJson() == null || area.getRulesJson().isBlank()) {
            return AreaRules.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(area.getRulesJson());
            UUID defaultStoreId = root.hasNonNull("defaultStoreId")
                    ? UUID.fromString(root.get("defaultStoreId").asText())
                    : null;
            Integer maxDistanceMeters = root.hasNonNull("maxDistanceMeters")
                    ? root.get("maxDistanceMeters").asInt()
                    : null;
            boolean pickupOnly = root.path("pickupOnly").asBoolean(false);
            boolean deliveryOnly = root.path("deliveryOnly").asBoolean(false);
            return new AreaRules(defaultStoreId, maxDistanceMeters, pickupOnly, deliveryOnly);
        } catch (Exception ex) {
            log.warn("Invalid service area rules for area {}: {}", area.getId(), ex.getMessage());
            return AreaRules.empty();
        }
    }

    private record CandidateStore(Store store, long distanceMeters, boolean openNow) {
    }

    private record AreaRules(UUID defaultStoreId, Integer maxDistanceMeters, boolean pickupOnly, boolean deliveryOnly) {
        private static AreaRules empty() {
            return new AreaRules(null, null, false, false);
        }

        private boolean disallows(StoreServiceType serviceType) {
            if (serviceType == null) return false;
            if (pickupOnly && serviceType != StoreServiceType.PICKUP) {
                return true;
            }
            return deliveryOnly && serviceType != StoreServiceType.DELIVERY;
        }
    }
}
