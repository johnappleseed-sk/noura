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
 * FedEx shipping carrier integration.
 * Uses FedEx REST API for rates, labels, and tracking.
 */
@Component
public class FedExShippingCarrier implements ShippingCarrier {
    private static final Logger log = LoggerFactory.getLogger(FedExShippingCarrier.class);
    private static final String CARRIER_ID = "fedex";

    private final boolean enabled;
    private final String apiKey;
    private final String secretKey;
    private final String accountNumber;
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String accessToken;
    private LocalDateTime tokenExpiry;

    public FedExShippingCarrier(
            @Value("${app.shipping.fedex.enabled:false}") boolean enabled,
            @Value("${app.shipping.fedex.api-key:}") String apiKey,
            @Value("${app.shipping.fedex.secret-key:}") String secretKey,
            @Value("${app.shipping.fedex.account-number:}") String accountNumber,
            @Value("${app.shipping.fedex.base-url:https://apis.fedex.com}") String baseUrl,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.accountNumber = accountNumber;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();

        if (enabled && isConfigured()) {
            log.info("FedEx shipping carrier initialized");
        }
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && secretKey != null && !secretKey.isBlank()
                && accountNumber != null && !accountNumber.isBlank();
    }

    @Override
    public String getCarrierId() {
        return CARRIER_ID;
    }

    @Override
    public String getDisplayName() {
        return "FedEx";
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
                    baseUrl + "/rate/v1/rates/quotes",
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            return parseRateResponse(response.getBody());

        } catch (Exception e) {
            log.error("FedEx getRates failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public ShipmentResult createShipment(CreateShipmentRequest request) {
        if (!isEnabled()) {
            return ShipmentResult.failure("NOT_CONFIGURED", "FedEx is not configured");
        }

        try {
            ensureValidToken();

            Map<String, Object> shipRequest = buildShipmentRequest(request);

            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(shipRequest, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/ship/v1/shipments",
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            return parseShipmentResponse(response.getBody());

        } catch (Exception e) {
            log.error("FedEx createShipment failed: {}", e.getMessage());
            return ShipmentResult.failure("API_ERROR", e.getMessage());
        }
    }

    @Override
    public TrackingInfo getTracking(String trackingNumber) {
        if (!isEnabled()) {
            return TrackingInfo.notFound(trackingNumber);
        }

        try {
            ensureValidToken();

            Map<String, Object> trackRequest = Map.of(
                    "trackingInfo", List.of(Map.of(
                            "trackingNumberInfo", Map.of(
                                    "trackingNumber", trackingNumber
                            )
                    )),
                    "includeDetailedScans", true
            );

            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(trackRequest, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/track/v1/trackingnumbers",
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            return parseTrackingResponse(response.getBody(), trackingNumber);

        } catch (Exception e) {
            log.error("FedEx getTracking failed for {}: {}", trackingNumber, e.getMessage());
            return TrackingInfo.error(trackingNumber, e.getMessage());
        }
    }

    @Override
    public CancelResult cancelShipment(String trackingNumber) {
        if (!isEnabled()) {
            return CancelResult.failure("NOT_CONFIGURED", "FedEx is not configured");
        }

        try {
            ensureValidToken();

            Map<String, Object> cancelRequest = Map.of(
                    "accountNumber", Map.of("value", accountNumber),
                    "trackingNumber", trackingNumber
            );

            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(cancelRequest, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/ship/v1/shipments/cancel",
                    HttpMethod.PUT,
                    entity,
                    JsonNode.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return CancelResult.success(trackingNumber);
            } else {
                return CancelResult.failure("CANCEL_FAILED", "Unable to cancel shipment");
            }

        } catch (Exception e) {
            log.error("FedEx cancelShipment failed for {}: {}", trackingNumber, e.getMessage());
            return CancelResult.failure("API_ERROR", e.getMessage());
        }
    }

    // === Authentication ===

    private void ensureValidToken() {
        if (accessToken != null && tokenExpiry != null && LocalDateTime.now().isBefore(tokenExpiry)) {
            return; // Token still valid
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "grant_type=client_credentials&client_id=" + apiKey + "&client_secret=" + secretKey;

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/oauth/token",
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            JsonNode responseBody = response.getBody();
            if (responseBody != null) {
                accessToken = responseBody.path("access_token").asText();
                int expiresIn = responseBody.path("expires_in").asInt(3600);
                tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn - 60); // 60s buffer
                log.debug("FedEx OAuth token refreshed, expires in {} seconds", expiresIn);
            }

        } catch (Exception e) {
            log.error("Failed to obtain FedEx OAuth token: {}", e.getMessage());
            throw new RuntimeException("FedEx authentication failed", e);
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("X-locale", "en_US");
        return headers;
    }

    // === Request Builders ===

    private Map<String, Object> buildRateRequest(RateRequest request) {
        return Map.of(
                "accountNumber", Map.of("value", accountNumber),
                "requestedShipment", Map.of(
                        "shipper", buildAddress(request.originAddress()),
                        "recipient", buildAddress(request.destinationAddress()),
                        "pickupType", "DROPOFF_AT_FEDEX_LOCATION",
                        "rateRequestType", List.of("LIST", "ACCOUNT"),
                        "requestedPackageLineItems", List.of(
                                Map.of(
                                        "weight", Map.of(
                                                "units", "LB",
                                                "value", request.weightLbs()
                                        ),
                                        "dimensions", Map.of(
                                                "length", request.lengthIn(),
                                                "width", request.widthIn(),
                                                "height", request.heightIn(),
                                                "units", "IN"
                                        )
                                )
                        )
                )
        );
    }

    private Map<String, Object> buildShipmentRequest(CreateShipmentRequest request) {
        return Map.of(
                "accountNumber", Map.of("value", accountNumber),
                "labelResponseOptions", "URL_ONLY",
                "requestedShipment", Map.of(
                        "shipper", buildContactAddress(request.shipper()),
                        "recipients", List.of(buildContactAddress(request.recipient())),
                        "pickupType", "DROPOFF_AT_FEDEX_LOCATION",
                        "serviceType", request.serviceType(),
                        "packagingType", "YOUR_PACKAGING",
                        "labelSpecification", Map.of(
                                "labelStockType", "PAPER_4X6",
                                "imageType", "PDF"
                        ),
                        "requestedPackageLineItems", List.of(
                                Map.of(
                                        "weight", Map.of(
                                                "units", "LB",
                                                "value", request.weightLbs()
                                        )
                                )
                        )
                )
        );
    }

    private Map<String, Object> buildAddress(Address address) {
        return Map.of(
                "address", Map.of(
                        "streetLines", List.of(address.line1(), address.line2() != null ? address.line2() : ""),
                        "city", address.city(),
                        "stateOrProvinceCode", address.stateCode(),
                        "postalCode", address.postalCode(),
                        "countryCode", address.countryCode()
                )
        );
    }

    private Map<String, Object> buildContactAddress(ContactAddress contact) {
        Map<String, Object> address = new HashMap<>(buildAddress(contact.address()));
        address.put("contact", Map.of(
                "personName", contact.name(),
                "phoneNumber", contact.phone(),
                "emailAddress", contact.email() != null ? contact.email() : ""
        ));
        return address;
    }

    // === Response Parsers ===

    private List<ShippingRate> parseRateResponse(JsonNode response) {
        List<ShippingRate> rates = new ArrayList<>();

        JsonNode rateReplyDetails = response.path("output").path("rateReplyDetails");
        if (rateReplyDetails.isArray()) {
            for (JsonNode detail : rateReplyDetails) {
                String serviceType = detail.path("serviceType").asText();
                String serviceName = detail.path("serviceName").asText();

                JsonNode ratedShipment = detail.path("ratedShipmentDetails").get(0);
                if (ratedShipment != null) {
                    BigDecimal totalCharge = new BigDecimal(
                            ratedShipment.path("totalNetCharge").asText("0")
                    );
                    String currency = ratedShipment.path("currency").asText("USD");

                    // Estimate delivery date
                    String transitDays = detail.path("commit").path("transitDays").path("description").asText("");

                    rates.add(new ShippingRate(
                            CARRIER_ID,
                            serviceType,
                            serviceName,
                            totalCharge,
                            currency,
                            transitDays,
                            null // delivery date would need calculation
                    ));
                }
            }
        }

        return rates;
    }

    private ShipmentResult parseShipmentResponse(JsonNode response) {
        JsonNode output = response.path("output").path("transactionShipments").get(0);
        if (output == null) {
            return ShipmentResult.failure("PARSE_ERROR", "Invalid response from FedEx");
        }

        String masterTrackingNumber = output.path("masterTrackingNumber").asText();
        JsonNode pieceResponse = output.path("pieceResponses").get(0);

        String trackingNumber = pieceResponse != null
                ? pieceResponse.path("trackingNumber").asText()
                : masterTrackingNumber;

        String labelUrl = pieceResponse != null
                ? pieceResponse.path("packageDocuments").get(0).path("url").asText()
                : null;

        return ShipmentResult.success(trackingNumber, labelUrl, response.toString());
    }

    private TrackingInfo parseTrackingResponse(JsonNode response, String trackingNumber) {
        JsonNode trackResults = response.path("output").path("completeTrackResults").get(0)
                .path("trackResults").get(0);

        if (trackResults == null || trackResults.isMissingNode()) {
            return TrackingInfo.notFound(trackingNumber);
        }

        String status = trackResults.path("latestStatusDetail").path("statusByLocale").asText();
        String statusCode = trackResults.path("latestStatusDetail").path("code").asText();

        List<TrackingEvent> events = new ArrayList<>();
        JsonNode scanEvents = trackResults.path("scanEvents");
        if (scanEvents.isArray()) {
            for (JsonNode event : scanEvents) {
                events.add(new TrackingEvent(
                        event.path("date").asText(),
                        event.path("eventDescription").asText(),
                        event.path("scanLocation").path("city").asText() + ", " +
                                event.path("scanLocation").path("stateOrProvinceCode").asText()
                ));
            }
        }

        TrackingStatus trackingStatus = mapFedExStatus(statusCode);

        return new TrackingInfo(
                trackingNumber,
                CARRIER_ID,
                trackingStatus,
                status,
                events,
                null // estimatedDelivery
        );
    }

    private TrackingStatus mapFedExStatus(String code) {
        return switch (code) {
            case "IT" -> TrackingStatus.IN_TRANSIT;
            case "DL" -> TrackingStatus.DELIVERED;
            case "OD" -> TrackingStatus.OUT_FOR_DELIVERY;
            case "PU" -> TrackingStatus.PICKED_UP;
            case "DE", "SE", "CA" -> TrackingStatus.EXCEPTION;
            default -> TrackingStatus.UNKNOWN;
        };
    }
}
