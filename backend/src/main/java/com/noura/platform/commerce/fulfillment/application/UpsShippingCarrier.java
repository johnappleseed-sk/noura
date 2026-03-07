package com.noura.platform.commerce.fulfillment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * UPS shipping carrier integration.
 * Uses UPS REST API for rates, labels, and tracking.
 */
@Component
public class UpsShippingCarrier implements ShippingCarrier {
    private static final Logger log = LoggerFactory.getLogger(UpsShippingCarrier.class);
    private static final String CARRIER_ID = "ups";

    private final boolean enabled;
    private final String clientId;
    private final String clientSecret;
    private final String accountNumber;
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String accessToken;
    private LocalDateTime tokenExpiry;

    public UpsShippingCarrier(
            @Value("${app.shipping.ups.enabled:false}") boolean enabled,
            @Value("${app.shipping.ups.client-id:}") String clientId,
            @Value("${app.shipping.ups.client-secret:}") String clientSecret,
            @Value("${app.shipping.ups.account-number:}") String accountNumber,
            @Value("${app.shipping.ups.base-url:https://onlinetools.ups.com}") String baseUrl,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.accountNumber = accountNumber;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();

        if (enabled && isConfigured()) {
            log.info("UPS shipping carrier initialized");
        }
    }

    private boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && accountNumber != null && !accountNumber.isBlank();
    }

    @Override
    public String getCarrierId() {
        return CARRIER_ID;
    }

    @Override
    public String getDisplayName() {
        return "UPS";
    }

    @Override
    public boolean isEnabled() {
        return enabled && isConfigured();
    }

    @Override
    public List<ShippingRate> getRates(RateRequest request) {
        if (!isEnabled()) {
            return Collections.emptyList();
        }

        try {
            ensureValidToken();

            Map<String, Object> rateRequest = buildRateRequest(request);

            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(rateRequest, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/api/rating/v1/Shop",
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            return parseRateResponse(response.getBody());

        } catch (Exception e) {
            log.error("UPS getRates failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public ShipmentResult createShipment(CreateShipmentRequest request) {
        if (!isEnabled()) {
            return ShipmentResult.failure("NOT_CONFIGURED", "UPS is not configured");
        }

        try {
            ensureValidToken();

            Map<String, Object> shipRequest = buildShipmentRequest(request);

            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(shipRequest, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/api/shipments/v1/ship",
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            return parseShipmentResponse(response.getBody());

        } catch (Exception e) {
            log.error("UPS createShipment failed: {}", e.getMessage());
            return ShipmentResult.failure("API_ERROR", e.getMessage());
        }
    }

    @Override
    public TrackingInfo getTracking(String trackingNumber) {
        if (!isEnabled()) {
            return new TrackingInfo(trackingNumber, TrackingStatus.UNKNOWN,
                    "UPS not configured", null, null, null, List.of());
        }

        try {
            ensureValidToken();

            HttpHeaders headers = createAuthHeaders();
            headers.set("transId", UUID.randomUUID().toString());
            headers.set("transactionSrc", "noura_platform");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/api/track/v1/details/" + trackingNumber,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            return parseTrackingResponse(response.getBody(), trackingNumber);

        } catch (Exception e) {
            log.error("UPS getTracking failed for {}: {}", trackingNumber, e.getMessage());
            return new TrackingInfo(trackingNumber, TrackingStatus.EXCEPTION,
                    e.getMessage(), null, null, null, List.of());
        }
    }

    @Override
    public CancelResult cancelShipment(String trackingNumber) {
        if (!isEnabled()) {
            return new CancelResult(false, "UPS is not configured");
        }

        try {
            ensureValidToken();

            HttpHeaders headers = createAuthHeaders();

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/api/shipments/v1/void/cancel/" + trackingNumber,
                    HttpMethod.DELETE,
                    entity,
                    JsonNode.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return new CancelResult(true, "Shipment " + trackingNumber + " cancelled");
            } else {
                return new CancelResult(false, "Unable to cancel shipment");
            }

        } catch (Exception e) {
            log.error("UPS cancelShipment failed for {}: {}", trackingNumber, e.getMessage());
            return new CancelResult(false, e.getMessage());
        }
    }

    // === Authentication ===

    private void ensureValidToken() {
        if (accessToken != null && tokenExpiry != null && LocalDateTime.now().isBefore(tokenExpiry)) {
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            String body = "grant_type=client_credentials";

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/security/v1/oauth/token",
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            JsonNode responseBody = response.getBody();
            if (responseBody != null) {
                accessToken = responseBody.path("access_token").asText();
                int expiresIn = responseBody.path("expires_in").asInt(3600);
                tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn - 60);
                log.debug("UPS OAuth token refreshed, expires in {} seconds", expiresIn);
            }

        } catch (Exception e) {
            log.error("Failed to obtain UPS OAuth token: {}", e.getMessage());
            throw new RuntimeException("UPS authentication failed", e);
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    // === Request Builders ===

    private Map<String, Object> buildRateRequest(RateRequest request) {
        Address from = request.fromAddress();
        Address to = request.toAddress();

        List<Map<String, Object>> packageList = new ArrayList<>();
        for (PackageInfo pkg : request.packages()) {
            Map<String, Object> pkgMap = new HashMap<>();
            pkgMap.put("PackagingType", Map.of("Code", "02"));
            pkgMap.put("Dimensions", Map.of(
                    "UnitOfMeasurement", Map.of("Code", "CM"),
                    "Length", pkg.lengthCm().toPlainString(),
                    "Width", pkg.widthCm().toPlainString(),
                    "Height", pkg.heightCm().toPlainString()
            ));
            pkgMap.put("PackageWeight", Map.of(
                    "UnitOfMeasurement", Map.of("Code", "KGS"),
                    "Weight", pkg.weightKg().toPlainString()
            ));
            packageList.add(pkgMap);
        }

        Map<String, Object> shipment = new HashMap<>();
        shipment.put("Shipper", buildUpsAddress(from, accountNumber));
        shipment.put("ShipTo", buildUpsAddress(to, null));
        shipment.put("ShipFrom", buildUpsAddress(from, null));
        shipment.put("Package", packageList);

        return Map.of(
                "RateRequest", Map.of(
                        "Request", Map.of("RequestOption", "Shop"),
                        "Shipment", shipment
                )
        );
    }

    private Map<String, Object> buildShipmentRequest(CreateShipmentRequest request) {
        Address from = request.fromAddress();
        Address to = request.toAddress();

        Map<String, Object> shipment = new HashMap<>();
        shipment.put("Description", "Shipment");
        shipment.put("Shipper", buildUpsAddress(from, accountNumber));
        shipment.put("ShipTo", buildUpsAddress(to, null));
        shipment.put("ShipFrom", buildUpsAddress(from, null));
        shipment.put("PaymentInformation", Map.of(
                "ShipmentCharge", Map.of(
                        "Type", "01",
                        "BillShipper", Map.of("AccountNumber", accountNumber)
                )
        ));
        shipment.put("Service", Map.of("Code", mapServiceType(request.serviceCode())));

        List<Map<String, Object>> packageList = new ArrayList<>();
        for (PackageInfo pkg : request.packages()) {
            packageList.add(Map.of(
                    "PackagingType", Map.of("Code", "02"),
                    "PackageWeight", Map.of(
                            "UnitOfMeasurement", Map.of("Code", "KGS"),
                            "Weight", pkg.weightKg().toPlainString()
                    )
            ));
        }
        shipment.put("Package", packageList);

        return Map.of(
                "ShipmentRequest", Map.of(
                        "Request", Map.of("RequestOption", "validate"),
                        "Shipment", shipment,
                        "LabelSpecification", Map.of(
                                "LabelImageFormat", Map.of("Code", "PDF"),
                                "LabelStockSize", Map.of("Height", "6", "Width", "4")
                        )
                )
        );
    }

    private Map<String, Object> buildUpsAddress(Address address, String shipperNumber) {
        Map<String, Object> result = new HashMap<>();
        result.put("Address", Map.of(
                "AddressLine", List.of(address.line1(),
                        address.line2() != null ? address.line2() : ""),
                "City", address.city(),
                "StateProvinceCode", address.stateProvince() != null ? address.stateProvince() : "",
                "PostalCode", address.postalCode(),
                "CountryCode", address.countryCode()
        ));
        if (address.name() != null) {
            result.put("Name", address.name());
        }
        if (address.phone() != null) {
            result.put("Phone", Map.of("Number", address.phone()));
        }
        if (address.email() != null) {
            result.put("EMailAddress", address.email());
        }
        if (shipperNumber != null) {
            result.put("ShipperNumber", shipperNumber);
        }
        return result;
    }

    private String mapServiceType(String serviceType) {
        if (serviceType == null) return "03";
        return switch (serviceType.toUpperCase()) {
            case "GROUND", "UPS_GROUND", "03" -> "03";
            case "3_DAY_SELECT", "UPS_3_DAY_SELECT", "12" -> "12";
            case "2ND_DAY_AIR", "UPS_2ND_DAY_AIR", "02" -> "02";
            case "NEXT_DAY_AIR", "UPS_NEXT_DAY_AIR", "01" -> "01";
            case "NEXT_DAY_AIR_SAVER", "13" -> "13";
            case "WORLDWIDE_EXPRESS", "UPS_WORLDWIDE_EXPRESS", "07" -> "07";
            case "WORLDWIDE_EXPEDITED", "08" -> "08";
            default -> "03";
        };
    }

    // === Response Parsers ===

    private List<ShippingRate> parseRateResponse(JsonNode response) {
        List<ShippingRate> rates = new ArrayList<>();

        JsonNode ratedShipments = response.path("RateResponse").path("RatedShipment");
        if (ratedShipments.isArray()) {
            for (JsonNode rated : ratedShipments) {
                String serviceCode = rated.path("Service").path("Code").asText();
                String serviceName = mapUpsServiceName(serviceCode);

                BigDecimal totalCharge = new BigDecimal(
                        rated.path("TotalCharges").path("MonetaryValue").asText("0")
                );
                String currency = rated.path("TotalCharges").path("CurrencyCode").asText("USD");

                int transitDays = rated.path("GuaranteedDelivery")
                        .path("BusinessDaysInTransit").asInt(0);

                rates.add(new ShippingRate(
                        serviceCode,
                        serviceName,
                        totalCharge,
                        currency,
                        transitDays > 0 ? transitDays : 1,
                        transitDays > 0 ? transitDays + 2 : 5,
                        null
                ));
            }
        }

        return rates;
    }

    private ShipmentResult parseShipmentResponse(JsonNode response) {
        JsonNode shipmentResults = response.path("ShipmentResponse").path("ShipmentResults");

        String trackingNumber = shipmentResults.path("PackageResults")
                .path(0).path("TrackingNumber").asText();

        String totalCharge = shipmentResults.path("ShipmentCharges")
                .path("TotalCharges").path("MonetaryValue").asText("0");
        String currency = shipmentResults.path("ShipmentCharges")
                .path("TotalCharges").path("CurrencyCode").asText("USD");

        if (trackingNumber != null && !trackingNumber.isEmpty()) {
            return ShipmentResult.success(trackingNumber, null,
                    new BigDecimal(totalCharge), currency, null);
        } else {
            return ShipmentResult.failure("PARSE_ERROR", "Could not parse tracking number");
        }
    }

    private TrackingInfo parseTrackingResponse(JsonNode response, String trackingNumber) {
        JsonNode trackResponse = response.path("trackResponse").path("shipment").path(0);

        if (trackResponse.isMissingNode()) {
            return new TrackingInfo(trackingNumber, TrackingStatus.UNKNOWN,
                    "Not found", null, null, null, List.of());
        }

        JsonNode pkg = trackResponse.path("package").path(0);
        JsonNode currentStatus = pkg.path("currentStatus");

        String status = currentStatus.path("description").asText();
        String statusCode = currentStatus.path("code").asText();

        List<TrackingEvent> events = new ArrayList<>();
        JsonNode activities = pkg.path("activity");
        if (activities.isArray()) {
            for (JsonNode activity : activities) {
                JsonNode location = activity.path("location").path("address");
                String city = location.path("city").asText();
                String state = location.path("stateProvince").asText();

                LocalDateTime timestamp = parseUpsDateTime(
                        activity.path("date").asText(),
                        activity.path("time").asText());

                TrackingStatus eventStatus = mapUpsStatus(
                        activity.path("status").path("code").asText());

                events.add(new TrackingEvent(
                        timestamp,
                        city + ", " + state,
                        activity.path("status").path("description").asText(),
                        eventStatus
                ));
            }
        }

        TrackingStatus trackingStatus = mapUpsStatus(statusCode);

        return new TrackingInfo(
                trackingNumber,
                trackingStatus,
                status,
                null,
                null,
                null,
                events
        );
    }

    private LocalDateTime parseUpsDateTime(String date, String time) {
        try {
            // UPS format: YYYYMMDD and HHMMSS
            if (date.length() >= 8 && time.length() >= 6) {
                return LocalDateTime.of(
                        Integer.parseInt(date.substring(0, 4)),
                        Integer.parseInt(date.substring(4, 6)),
                        Integer.parseInt(date.substring(6, 8)),
                        Integer.parseInt(time.substring(0, 2)),
                        Integer.parseInt(time.substring(2, 4)),
                        Integer.parseInt(time.substring(4, 6))
                );
            }
        } catch (Exception e) {
            log.debug("Failed to parse UPS date/time: {} {}", date, time);
        }
        return LocalDateTime.now();
    }

    private String mapUpsServiceName(String code) {
        return switch (code) {
            case "01" -> "UPS Next Day Air";
            case "02" -> "UPS 2nd Day Air";
            case "03" -> "UPS Ground";
            case "07" -> "UPS Worldwide Express";
            case "08" -> "UPS Worldwide Expedited";
            case "12" -> "UPS 3 Day Select";
            case "13" -> "UPS Next Day Air Saver";
            case "14" -> "UPS Next Day Air Early";
            default -> "UPS Service " + code;
        };
    }

    private TrackingStatus mapUpsStatus(String code) {
        return switch (code) {
            case "I" -> TrackingStatus.IN_TRANSIT;
            case "D" -> TrackingStatus.DELIVERED;
            case "O" -> TrackingStatus.OUT_FOR_DELIVERY;
            case "P" -> TrackingStatus.PICKED_UP;
            case "X", "RS" -> TrackingStatus.EXCEPTION;
            case "M" -> TrackingStatus.LABEL_CREATED;
            default -> TrackingStatus.UNKNOWN;
        };
    }
}
