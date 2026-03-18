package com.noura.shipping.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.shipping.domain.entity.ServiceAreaRecord;
import com.noura.shipping.domain.entity.StoreRecord;
import com.noura.shipping.domain.enums.ServiceAreaStatus;
import com.noura.shipping.domain.enums.ServiceAreaType;
import com.noura.shipping.domain.enums.StoreServiceType;
import com.noura.shipping.domain.enums.StoreStatus;
import com.noura.shipping.domain.enums.StoreType;
import com.noura.shipping.dto.network.ServiceAreaValidationRequest;
import com.noura.shipping.dto.network.ServiceEligibilityResponse;
import com.noura.shipping.dto.network.StoreResponse;
import com.noura.shipping.repository.MerchantRecordRepository;
import com.noura.shipping.repository.ServiceAreaRecordRepository;
import com.noura.shipping.repository.StoreRecordRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FulfillmentNetworkServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class FulfillmentNetworkServiceImplTest {

    @Mock
    private MerchantRecordRepository merchantRecordRepository;

    @Mock
    private StoreRecordRepository storeRecordRepository;

    @Mock
    private ServiceAreaRecordRepository serviceAreaRecordRepository;

    /**
     * Verifies radius-based validation picks the assigned active store and reports success.
     */
    @Test
    void shouldValidateRadiusServiceAreaAgainstAssignedStore() {
        FulfillmentNetworkServiceImpl service = new FulfillmentNetworkServiceImpl(
                merchantRecordRepository,
                storeRecordRepository,
                serviceAreaRecordRepository,
                new ObjectMapper()
        );
        UUID storeId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID serviceAreaId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        StoreRecord store = store(storeId, new BigDecimal("11.56100000"), new BigDecimal("104.91600000"));
        ServiceAreaRecord area = radiusArea(serviceAreaId, storeId);

        when(serviceAreaRecordRepository.findByDeletedAtIsNullAndStatus(ServiceAreaStatus.ACTIVE)).thenReturn(List.of(area));
        when(storeRecordRepository.findById(storeId)).thenReturn(Optional.of(store));

        ServiceEligibilityResponse response = service.validateServiceArea(
                new ServiceAreaValidationRequest(
                        new BigDecimal("11.56100000"),
                        new BigDecimal("104.91600000"),
                        StoreServiceType.DELIVERY,
                        null,
                        5_000L
                )
        );

        Assertions.assertTrue(response.serviceAvailable());
        Assertions.assertEquals(serviceAreaId, response.matchedServiceAreaId());
        Assertions.assertEquals(storeId, response.matchedStoreId());
        Assertions.assertEquals("MATCHED", response.eligibilityReason());
    }

    /**
     * Verifies nearest-store ordering uses computed distance from the requested coordinates.
     */
    @Test
    void shouldReturnNearestStoresOrderedByDistance() {
        FulfillmentNetworkServiceImpl service = new FulfillmentNetworkServiceImpl(
                merchantRecordRepository,
                storeRecordRepository,
                serviceAreaRecordRepository,
                new ObjectMapper()
        );
        StoreRecord closer = store(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                new BigDecimal("11.56000000"),
                new BigDecimal("104.91500000")
        );
        closer.setName("Closer");
        closer.setStoreCode("STR-CLOSE");

        StoreRecord farther = store(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                new BigDecimal("11.59000000"),
                new BigDecimal("104.95000000")
        );
        farther.setName("Farther");
        farther.setStoreCode("STR-FAR");

        when(storeRecordRepository.findByDeletedAtIsNullAndStatus(StoreStatus.ACTIVE)).thenReturn(List.of(farther, closer));

        List<StoreResponse> response = service.findNearestStores(
                new BigDecimal("11.56100000"),
                new BigDecimal("104.91600000"),
                5
        );

        Assertions.assertEquals(2, response.size());
        Assertions.assertEquals(closer.getId(), response.getFirst().id());
        Assertions.assertTrue(response.getFirst().distanceMeters() <= response.get(1).distanceMeters());
    }

    private StoreRecord store(UUID storeId, BigDecimal latitude, BigDecimal longitude) {
        StoreRecord entity = new StoreRecord();
        entity.setId(storeId);
        entity.setStoreCode("STR-" + storeId.toString().substring(0, 4).toUpperCase());
        entity.setName("Store " + storeId.toString().substring(0, 4));
        entity.setType(StoreType.BRANCH);
        entity.setStatus(StoreStatus.ACTIVE);
        entity.setLatitude(latitude);
        entity.setLongitude(longitude);
        entity.setOpenNow(true);
        entity.setSupportedServices(List.of(StoreServiceType.DELIVERY, StoreServiceType.PICKUP));
        entity.setCreatedAt(Instant.parse("2026-03-18T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-03-18T00:00:00Z"));
        return entity;
    }

    private ServiceAreaRecord radiusArea(UUID serviceAreaId, UUID storeId) {
        ServiceAreaRecord entity = new ServiceAreaRecord();
        entity.setId(serviceAreaId);
        entity.setName("Central radius");
        entity.setType(ServiceAreaType.RADIUS);
        entity.setStatus(ServiceAreaStatus.ACTIVE);
        entity.setCenterLatitude(new BigDecimal("11.56100000"));
        entity.setCenterLongitude(new BigDecimal("104.91600000"));
        entity.setRadiusMeters(5_000);
        entity.setStoreIds(List.of(storeId));
        entity.setCreatedAt(Instant.parse("2026-03-18T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-03-18T00:00:00Z"));
        return entity;
    }
}
