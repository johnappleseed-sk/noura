package com.noura.platform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.ServiceUnavailableException;
import com.noura.platform.dto.location.ForwardGeocodeRequest;
import com.noura.platform.dto.location.GeocodeResultDto;
import com.noura.platform.dto.location.ReverseGeocodeRequest;
import com.noura.platform.location.util.LocationCacheKeys;
import com.noura.platform.service.LocationGeocodingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class NominatimLocationGeocodingService implements LocationGeocodingService {
    private static final Logger log = LoggerFactory.getLogger(NominatimLocationGeocodingService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final String baseUrl;
    private final String userAgent;
    private final int defaultLimit;

    public NominatimLocationGeocodingService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${app.location.geocoding.base-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${app.location.geocoding.user-agent:noura-platform/1.0}") String userAgent,
            @Value("${app.location.geocoding.timeout-ms:3500}") long timeoutMs,
            @Value("${app.location.geocoding.default-limit:6}") int defaultLimit
    ) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.userAgent = userAgent == null || userAgent.isBlank() ? "noura-platform/1.0" : userAgent.trim();
        this.defaultLimit = Math.min(Math.max(defaultLimit, 1), 10);
        Duration timeout = Duration.ofMillis(Math.max(500, timeoutMs));
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

    @Override
    public GeocodeResultDto reverseGeocode(ReverseGeocodeRequest request) {
        return reverseGeocode(request.latitude(), request.longitude(), request.locale());
    }

    @Override
    public List<GeocodeResultDto> forwardGeocode(ForwardGeocodeRequest request) {
        int limit = request.limit() == null ? defaultLimit : Math.min(Math.max(request.limit(), 1), 10);
        return forwardGeocode(request.query(), request.countryCodes(), limit, request.locale());
    }

    @Cacheable(cacheNames = "geocodeReverse", key = "T(com.noura.platform.location.util.LocationCacheKeys).reverseKey(#latitude, #longitude)")
    public GeocodeResultDto reverseGeocode(BigDecimal latitude, BigDecimal longitude, String locale) {
        if (latitude == null || longitude == null) {
            throw new BadRequestException("GEO_COORDINATES_REQUIRED", "Latitude and longitude are required.");
        }

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/reverse")
                .queryParam("format", "jsonv2")
                .queryParam("addressdetails", 1)
                .queryParam("lat", latitude.toPlainString())
                .queryParam("lon", longitude.toPlainString())
                .build()
                .toUriString();

        try {
            String body = exchangeJson(url, locale);
            JsonNode root = objectMapper.readTree(body);
            return toDto(root);
        } catch (RestClientException ex) {
            log.warn("Reverse geocode request failed: {}", ex.getMessage());
            throw new ServiceUnavailableException("GEOCODE_UNAVAILABLE", "Geocoding service is temporarily unavailable.");
        } catch (Exception ex) {
            log.warn("Reverse geocode parsing failed: {}", ex.getMessage());
            throw new ServiceUnavailableException("GEOCODE_UNAVAILABLE", "Geocoding service is temporarily unavailable.");
        }
    }

    @Cacheable(cacheNames = "geocodeForward", key = "'q:' + #query + ':cc:' + #countryCodes + ':l:' + #limit + ':loc:' + #locale")
    public List<GeocodeResultDto> forwardGeocode(String query, String countryCodes, int limit, String locale) {
        String safeQuery = query == null ? "" : query.trim();
        if (safeQuery.isBlank()) {
            throw new BadRequestException("GEO_QUERY_REQUIRED", "Query is required.");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/search")
                .queryParam("format", "jsonv2")
                .queryParam("addressdetails", 1)
                .queryParam("limit", limit)
                .queryParam("q", safeQuery);

        if (countryCodes != null && !countryCodes.isBlank()) {
            builder.queryParam("countrycodes", countryCodes.trim());
        }

        String url = builder.build().toUriString();

        try {
            String body = exchangeJson(url, locale);
            JsonNode root = objectMapper.readTree(body);
            List<GeocodeResultDto> results = new ArrayList<>();
            if (root != null && root.isArray()) {
                for (JsonNode item : root) {
                    GeocodeResultDto dto = toDto(item);
                    if (dto != null) results.add(dto);
                }
            }
            return results;
        } catch (RestClientException ex) {
            log.warn("Forward geocode request failed: {}", ex.getMessage());
            throw new ServiceUnavailableException("GEOCODE_UNAVAILABLE", "Geocoding service is temporarily unavailable.");
        } catch (Exception ex) {
            log.warn("Forward geocode parsing failed: {}", ex.getMessage());
            throw new ServiceUnavailableException("GEOCODE_UNAVAILABLE", "Geocoding service is temporarily unavailable.");
        }
    }

    private String exchangeJson(String url, String locale) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, userAgent);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (locale != null && !locale.isBlank()) {
            try {
                headers.setAcceptLanguageAsLocales(List.of(Locale.forLanguageTag(locale.trim())));
            } catch (Exception ignored) {
                // ignore invalid locale
            }
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String body = response.getBody();
        if (body == null || body.isBlank()) {
            throw new ServiceUnavailableException("GEOCODE_EMPTY", "Geocoding service returned an empty response.");
        }
        return body;
    }

    private GeocodeResultDto toDto(JsonNode node) {
        if (node == null || node.isNull()) return null;
        BigDecimal lat = parseDecimal(node.get("lat"));
        BigDecimal lon = parseDecimal(node.get("lon"));
        String formatted = text(node.get("display_name"));
        String placeId = text(node.get("place_id"));

        JsonNode address = node.get("address");
        String country = address == null ? null : text(address.get("country"));
        String region = address == null ? null : firstNonBlank(address, "state", "region", "province");
        String city = address == null ? null : firstNonBlank(address, "city", "town", "village", "hamlet");
        String district = address == null ? null : firstNonBlank(address, "suburb", "city_district", "county", "district");
        String postalCode = address == null ? null : text(address.get("postcode"));

        return new GeocodeResultDto(lat, lon, formatted, country, region, city, district, postalCode, placeId);
    }

    private BigDecimal parseDecimal(JsonNode node) {
        String value = text(node);
        if (value == null) return null;
        try {
            return new BigDecimal(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstNonBlank(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node.get(field));
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String text(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String value = node.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String trimTrailingSlash(String input) {
        String base = Optional.ofNullable(input).orElse("").trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base.isEmpty() ? "https://nominatim.openstreetmap.org" : base;
    }
}

