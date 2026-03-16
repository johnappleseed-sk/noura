package com.noura.platform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.domain.entity.ServiceArea;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.enums.ServiceAreaStatus;
import com.noura.platform.domain.enums.ServiceAreaType;
import com.noura.platform.domain.enums.StoreServiceType;
import com.noura.platform.dto.location.NearbyStoreDto;
import com.noura.platform.dto.location.ServiceAreaValidationRequest;
import com.noura.platform.dto.location.ServiceEligibilityDto;
import com.noura.platform.repository.ServiceAreaRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.repository.UserLocationRepository;
import com.noura.platform.service.LocationGeocodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationIntelligenceServiceImplTest {

    @Mock
    private LocationGeocodingService geocodingService;

    @Mock
    private ServiceAreaRepository serviceAreaRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserLocationRepository userLocationRepository;

    private LocationIntelligenceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LocationIntelligenceServiceImpl(
                geocodingService,
                serviceAreaRepository,
                storeRepository,
                userAccountRepository,
                userLocationRepository,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "persistUserLocationEnabled", true);
        ReflectionTestUtils.setField(service, "persistPrecisionDecimals", 5);
        ReflectionTestUtils.setField(service, "maxTrustedAccuracyMeters", 200);
    }

    @Test
    void validateReturnsAvailableWhenPointMatchesRadiusAreaAndEligibleStore() {
        ServiceArea area = radiusArea("Metro", decimal("40.7128"), decimal("-74.0060"), 5_000, null);
        Store store = store(
                "Downtown",
                decimal("40.7130"),
                decimal("-74.0040"),
                3_000,
                Set.of(StoreServiceType.DELIVERY)
        );
        area.getStores().add(store);

        when(serviceAreaRepository.findByStatus(ServiceAreaStatus.ACTIVE)).thenReturn(List.of(area));

        ServiceEligibilityDto eligibility = service.validate(new ServiceAreaValidationRequest(
                decimal("40.7129"),
                decimal("-74.0055"),
                StoreServiceType.DELIVERY,
                Instant.parse("2026-03-10T10:15:30Z"),
                null
        ));

        assertThat(eligibility.serviceAvailable()).isTrue();
        assertThat(eligibility.matchedServiceAreaId()).isEqualTo(area.getId());
        assertThat(eligibility.matchedStoreId()).isEqualTo(store.getId());
        assertThat(eligibility.eligibilityReason()).isEqualTo("AVAILABLE");
        assertThat(eligibility.insideServiceArea()).isTrue();
        assertThat(eligibility.storeOpenNow()).isTrue();
    }

    @Test
    void validateRejectsPickupForDeliveryOnlyAreaRules() {
        ServiceArea area = radiusArea(
                "Delivery-only zone",
                decimal("40.7128"),
                decimal("-74.0060"),
                5_000,
                "{\"deliveryOnly\":true}"
        );
        Store store = store(
                "Downtown",
                decimal("40.7130"),
                decimal("-74.0040"),
                3_000,
                Set.of(StoreServiceType.DELIVERY, StoreServiceType.PICKUP)
        );
        area.getStores().add(store);

        when(serviceAreaRepository.findByStatus(ServiceAreaStatus.ACTIVE)).thenReturn(List.of(area));

        ServiceEligibilityDto eligibility = service.validate(new ServiceAreaValidationRequest(
                decimal("40.7129"),
                decimal("-74.0055"),
                StoreServiceType.PICKUP,
                Instant.parse("2026-03-10T10:15:30Z"),
                null
        ));

        assertThat(eligibility.serviceAvailable()).isFalse();
        assertThat(eligibility.matchedServiceAreaId()).isEqualTo(area.getId());
        assertThat(eligibility.eligibilityReason()).isEqualTo("DELIVERY_ONLY_AREA");
    }

    @Test
    void nearbyStoresFiltersDeliveryLocationsOutsideStoreRadius() {
        Store inRange = store(
                "Nearby",
                decimal("40.7129"),
                decimal("-74.0058"),
                1_000,
                Set.of(StoreServiceType.DELIVERY)
        );
        Store outOfRange = store(
                "Too far",
                decimal("40.7520"),
                decimal("-74.0400"),
                500,
                Set.of(StoreServiceType.DELIVERY)
        );

        when(storeRepository.findAll()).thenReturn(List.of(inRange, outOfRange));

        List<NearbyStoreDto> stores = service.nearbyStores(
                decimal("40.7128"),
                decimal("-74.0060"),
                StoreServiceType.DELIVERY,
                false,
                10,
                null
        );

        assertThat(stores)
                .extracting(NearbyStoreDto::id)
                .containsExactly(inRange.getId());
    }

    private ServiceArea radiusArea(String name, BigDecimal latitude, BigDecimal longitude, int radiusMeters, String rulesJson) {
        ServiceArea area = new ServiceArea();
        area.setId(UUID.randomUUID());
        area.setName(name);
        area.setType(ServiceAreaType.RADIUS);
        area.setStatus(ServiceAreaStatus.ACTIVE);
        area.setCenterLatitude(latitude);
        area.setCenterLongitude(longitude);
        area.setRadiusMeters(radiusMeters);
        area.setRulesJson(rulesJson);
        return area;
    }

    private Store store(String name, BigDecimal latitude, BigDecimal longitude, Integer serviceRadiusMeters, Set<StoreServiceType> services) {
        Store store = new Store();
        store.setId(UUID.randomUUID());
        store.setName(name);
        store.setAddressLine1("1 Main St");
        store.setCity("New York");
        store.setState("NY");
        store.setZipCode("10001");
        store.setCountry("US");
        store.setRegion("US-NY");
        store.setLatitude(latitude);
        store.setLongitude(longitude);
        store.setServiceRadiusMeters(serviceRadiusMeters);
        store.setOpenTime(LocalTime.MIN);
        store.setCloseTime(LocalTime.MAX.minusSeconds(1));
        store.setActive(true);
        store.setServices(services);
        store.setShippingFee(BigDecimal.ZERO);
        store.setFreeShippingThreshold(BigDecimal.ZERO);
        return store;
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
