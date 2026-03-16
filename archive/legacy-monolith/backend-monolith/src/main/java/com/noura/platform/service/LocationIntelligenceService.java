package com.noura.platform.service;

import com.noura.platform.domain.enums.StoreServiceType;
import com.noura.platform.dto.location.LocationResolveDto;
import com.noura.platform.dto.location.LocationResolveRequest;
import com.noura.platform.dto.location.NearbyStoreDto;
import com.noura.platform.dto.location.ServiceAreaValidationRequest;
import com.noura.platform.dto.location.ServiceEligibilityDto;

import java.math.BigDecimal;
import java.util.List;

public interface LocationIntelligenceService {
    ServiceEligibilityDto validate(ServiceAreaValidationRequest request);

    List<NearbyStoreDto> nearbyStores(BigDecimal latitude,
                                     BigDecimal longitude,
                                     StoreServiceType serviceType,
                                     Boolean openNow,
                                     Integer limit,
                                     Integer maxDistanceMeters);

    LocationResolveDto resolve(LocationResolveRequest request, String actor);
}

