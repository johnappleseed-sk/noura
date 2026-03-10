package com.noura.platform.service;

import com.noura.platform.dto.location.ForwardGeocodeRequest;
import com.noura.platform.dto.location.GeocodeResultDto;
import com.noura.platform.dto.location.ReverseGeocodeRequest;

import java.util.List;

/**
 * Abstracts geocoding provider integrations (OpenStreetMap/Nominatim compatible).
 *
 * <p>This keeps provider-specific payloads out of controllers and UIs.</p>
 */
public interface LocationGeocodingService {
    GeocodeResultDto reverseGeocode(ReverseGeocodeRequest request);

    List<GeocodeResultDto> forwardGeocode(ForwardGeocodeRequest request);
}

