package com.noura.shipping.provider;

import com.noura.shipping.config.RuleBasedCarrierProperties;
import com.noura.shipping.domain.enums.ShipmentStatus;
import com.noura.shipping.exception.ShippingOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic internal carrier used until external carrier integrations are added.
 *
 * <p>The rule engine intentionally stays simple: it exposes standard, express, and same-day
 * methods with clear subtotal, weight, and city eligibility rules so storefront and
 * orchestration flows can integrate now without committing to one external carrier.</p>
 */
@Component
@RequiredArgsConstructor
public class RuleBasedShippingCarrier implements ShippingCarrier {

    private static final String RULES_ALIAS = "rules";
    private static final String SANDBOX_ALIAS = "sandbox";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final RuleBasedCarrierProperties properties;

    /**
     * {@inheritDoc}
     */
    @Override
    public String carrierCode() {
        return normalizeCode(properties.getCarrierCode(), "rule-based");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String displayName() {
        String configured = properties.getDisplayName();
        return configured == null || configured.isBlank() ? "Noura Rule-Based Shipping" : configured.trim();
    }

    /**
     * Accepts rule-based aliases.
     *
     * @param requestedCarrierCode requested carrier code
     * @return {@code true} when this carrier can handle the code
     */
    @Override
    public boolean supports(String requestedCarrierCode) {
        if (requestedCarrierCode == null || requestedCarrierCode.isBlank()) {
            return false;
        }
        String normalized = requestedCarrierCode.trim().toLowerCase(Locale.ROOT);
        return carrierCode().equals(normalized) || RULES_ALIAS.equals(normalized) || SANDBOX_ALIAS.equals(normalized);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AvailableMethod> listAvailableMethods(MethodRequest request) {
        List<AvailableMethod> methods = new ArrayList<>();
        addIfAvailable(methods, buildStandardQuote(request));
        addIfAvailable(methods, buildExpressQuote(request));
        addIfAvailable(methods, buildSameDayQuote(request));
        return methods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QuoteResult quote(QuoteRequest request) {
        return switch (normalizeCode(request.methodCode(), null)) {
            case "standard" -> requiredQuote(buildStandardQuote(toMethodRequest(request)));
            case "express" -> requiredQuote(buildExpressQuote(toMethodRequest(request)));
            case "same_day" -> requiredQuote(buildSameDayQuote(toMethodRequest(request)));
            default -> throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "SHIPPING_METHOD_UNSUPPORTED",
                    "Unsupported shipping method: " + request.methodCode()
            );
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ShipmentCreationResult createShipment(CreateShipmentCommand request) {
        QuoteResult quote = quote(new QuoteRequest(
                request.recipientAddress(),
                request.cartSubtotal(),
                request.currencyCode(),
                totalItemCount(request.parcels()),
                totalWeight(request.parcels()),
                request.methodCode(),
                request.metadata()
        ));

        String scenario = resolveScenario(request.metadata());
        if ("create_exception".equals(scenario)) {
            return new ShipmentCreationResult(
                    ShipmentStatus.EXCEPTION,
                    deterministicExternalId(request.shipmentReference()),
                    null,
                    null,
                    quote.estimatedDeliveryAt(),
                    null,
                    "Rule-based carrier exception triggered by metadata"
            );
        }

        Instant labelCreatedAt = Instant.now();
        String trackingNumber = deterministicTrackingNumber(request.shipmentReference());
        return new ShipmentCreationResult(
                ShipmentStatus.LABEL_CREATED,
                deterministicExternalId(request.shipmentReference()),
                trackingNumber,
                "https://tracking.noura.local/track/" + trackingNumber,
                quote.estimatedDeliveryAt(),
                labelCreatedAt,
                null
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TrackingResult fetchShipmentStatus(StatusRequest request) {
        String scenario = resolveScenario(request.metadata());
        ShipmentStatus status = switch (scenario) {
            case "ready" -> ShipmentStatus.READY_FOR_FULFILLMENT;
            case "in_transit" -> ShipmentStatus.IN_TRANSIT;
            case "out_for_delivery" -> ShipmentStatus.OUT_FOR_DELIVERY;
            case "delivered" -> ShipmentStatus.DELIVERED;
            case "returned" -> ShipmentStatus.RETURNED;
            case "exception" -> ShipmentStatus.EXCEPTION;
            default -> request.currentStatus();
        };
        String trackingNumber = request.trackingNumber();
        Instant estimatedDeliveryAt = status == ShipmentStatus.DELIVERED
                ? Instant.now()
                : Instant.now().plus(2, ChronoUnit.DAYS);
        Instant deliveredAt = status == ShipmentStatus.DELIVERED ? Instant.now() : null;
        String failureReason = status == ShipmentStatus.EXCEPTION
                ? "Rule-based carrier exception triggered by metadata"
                : null;
        return new TrackingResult(
                status,
                trackingNumber,
                trackingNumber == null ? null : "https://tracking.noura.local/track/" + trackingNumber,
                estimatedDeliveryAt,
                deliveredAt,
                failureReason
        );
    }

    /**
     * Builds the standard-shipping quote when eligible.
     *
     * @param request normalized method request
     * @return available quote or {@code null}
     */
    private QuoteResult buildStandardQuote(MethodRequest request) {
        BigDecimal weight = normalizeWeight(request.totalWeightKg());
        if (weight.compareTo(new BigDecimal("30.0000")) > 0) {
            return null;
        }
        BigDecimal amount = calculateAmount(
                new BigDecimal("4.9900"),
                new BigDecimal("1.0000"),
                new BigDecimal("1.2500"),
                weight
        );
        String summary = "Base standard shipping plus weight surcharge after the first 1.0kg";
        if (normalizeMoney(request.cartSubtotal()).compareTo(normalizeMoney(properties.getFreeStandardThreshold())) >= 0) {
            amount = ZERO;
            summary = "Free standard shipping applied because cart subtotal reached the configured threshold";
        }
        return new QuoteResult(
                carrierCode(),
                "standard",
                "Standard Shipping",
                amount,
                normalizeCurrency(request.currencyCode()),
                3,
                5,
                Instant.now().plus(5, ChronoUnit.DAYS),
                summary
        );
    }

    /**
     * Builds the express-shipping quote when eligible.
     *
     * @param request normalized method request
     * @return available quote or {@code null}
     */
    private QuoteResult buildExpressQuote(MethodRequest request) {
        BigDecimal weight = normalizeWeight(request.totalWeightKg());
        if (weight.compareTo(new BigDecimal("20.0000")) > 0) {
            return null;
        }
        return new QuoteResult(
                carrierCode(),
                "express",
                "Express Shipping",
                calculateAmount(
                        new BigDecimal("9.9900"),
                        new BigDecimal("0.5000"),
                        new BigDecimal("2.5000"),
                        weight
                ),
                normalizeCurrency(request.currencyCode()),
                1,
                2,
                Instant.now().plus(2, ChronoUnit.DAYS),
                "Faster shipping with a higher weight surcharge after the first 0.5kg"
        );
    }

    /**
     * Builds the same-day-shipping quote when eligible.
     *
     * @param request normalized method request
     * @return available quote or {@code null}
     */
    private QuoteResult buildSameDayQuote(MethodRequest request) {
        BigDecimal weight = normalizeWeight(request.totalWeightKg());
        if (weight.compareTo(new BigDecimal("10.0000")) > 0) {
            return null;
        }
        String countryCode = normalizeCode(request.address() == null ? null : request.address().countryCode(), null);
        String city = normalizeCode(request.address() == null ? null : request.address().city(), null);
        if (countryCode == null
                || city == null
                || !normalizeCode(properties.getSameDayCountryCode(), "kh").equals(countryCode)
                || properties.getSameDayCities().stream()
                .map(value -> normalizeCode(value, null))
                .noneMatch(city::equals)) {
            return null;
        }
        return new QuoteResult(
                carrierCode(),
                "same_day",
                "Same Day Delivery",
                calculateAmount(
                        new BigDecimal("14.9900"),
                        new BigDecimal("0.5000"),
                        new BigDecimal("3.5000"),
                        weight
                ),
                normalizeCurrency(request.currencyCode()),
                0,
                1,
                Instant.now().plus(8, ChronoUnit.HOURS),
                "Same-day delivery is available only for configured cities in the configured domestic country"
        );
    }

    /**
     * Adds one available quote to the output list.
     *
     * @param methods target output list
     * @param quote candidate quote
     */
    private void addIfAvailable(List<AvailableMethod> methods, QuoteResult quote) {
        if (quote == null) {
            return;
        }
        methods.add(new AvailableMethod(
                quote.carrierCode(),
                quote.methodCode(),
                quote.methodName(),
                quote.amount(),
                quote.currencyCode(),
                quote.estimatedDaysMin(),
                quote.estimatedDaysMax(),
                quote.estimatedDeliveryAt(),
                true,
                quote.ruleSummary()
        ));
    }

    /**
     * Converts one quote request into the shared method-request shape used by the rule builders.
     *
     * @param request quote request
     * @return normalized method request
     */
    private MethodRequest toMethodRequest(QuoteRequest request) {
        return new MethodRequest(
                request.address(),
                request.cartSubtotal(),
                request.currencyCode(),
                request.itemCount(),
                request.totalWeightKg(),
                request.metadata()
        );
    }

    /**
     * Fails when a selected quote is not eligible.
     *
     * @param quote quote candidate
     * @return quote when available
     */
    private QuoteResult requiredQuote(QuoteResult quote) {
        if (quote == null) {
            throw new ShippingOperationException(
                    HttpStatus.BAD_REQUEST,
                    "SHIPPING_METHOD_UNAVAILABLE",
                    "Selected shipping method is not available for the requested destination or parcel constraints"
            );
        }
        return quote;
    }

    /**
     * Calculates a rule-based quote amount.
     *
     * @param base base amount
     * @param includedWeight included weight before surcharge
     * @param perKgSurcharge surcharge per additional kilogram
     * @param weight shipment weight
     * @return normalized quote amount
     */
    private BigDecimal calculateAmount(BigDecimal base, BigDecimal includedWeight, BigDecimal perKgSurcharge, BigDecimal weight) {
        BigDecimal additionalWeight = weight.subtract(includedWeight);
        if (additionalWeight.compareTo(BigDecimal.ZERO) < 0) {
            additionalWeight = BigDecimal.ZERO;
        }
        return base.add(additionalWeight.multiply(perKgSurcharge)).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Sums parcel quantities.
     *
     * @param parcels parcel list
     * @return total item count
     */
    private int totalItemCount(List<Parcel> parcels) {
        if (parcels == null || parcels.isEmpty()) {
            return 0;
        }
        return parcels.stream()
                .map(Parcel::quantity)
                .filter(value -> value != null && value > 0)
                .reduce(0, Integer::sum);
    }

    /**
     * Sums parcel weights multiplied by quantity.
     *
     * @param parcels parcel list
     * @return total weight
     */
    private BigDecimal totalWeight(List<Parcel> parcels) {
        if (parcels == null || parcels.isEmpty()) {
            return ZERO;
        }
        return parcels.stream()
                .map(parcel -> {
                    BigDecimal weight = parcel.weightKg() == null ? ZERO : parcel.weightKg();
                    int quantity = parcel.quantity() == null || parcel.quantity() < 1 ? 1 : parcel.quantity();
                    return weight.multiply(BigDecimal.valueOf(quantity));
                })
                .reduce(ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Resolves a deterministic scenario from metadata.
     *
     * @param metadata metadata map
     * @return normalized scenario code or {@code null}
     */
    private String resolveScenario(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Object direct = metadata.get("shippingScenario");
        if (direct == null) {
            direct = metadata.get("trackingScenario");
        }
        if (direct == null) {
            direct = metadata.get("carrierScenario");
        }
        if (direct == null) {
            return null;
        }
        return normalizeCode(String.valueOf(direct), null);
    }

    /**
     * Generates a deterministic external shipment ID from one shipment reference.
     *
     * @param shipmentReference shipment reference
     * @return external shipment ID
     */
    private String deterministicExternalId(String shipmentReference) {
        return "ship_" + UUID.nameUUIDFromBytes(shipmentReference.getBytes()).toString().replace("-", "");
    }

    /**
     * Generates a deterministic tracking number from one shipment reference.
     *
     * @param shipmentReference shipment reference
     * @return tracking number
     */
    private String deterministicTrackingNumber(String shipmentReference) {
        String suffix = UUID.nameUUIDFromBytes((shipmentReference + "-tracking").getBytes())
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
        return "NRA" + suffix;
    }

    /**
     * Normalizes monetary values to scale 4.
     *
     * @param value monetary value
     * @return normalized amount
     */
    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Normalizes total weight to scale 4.
     *
     * @param value weight value
     * @return normalized weight
     */
    private BigDecimal normalizeWeight(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Normalizes one carrier or method code.
     *
     * @param value source value
     * @param fallback fallback when blank
     * @return normalized code
     */
    private String normalizeCode(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes currency code to uppercase.
     *
     * @param value currency code
     * @return normalized currency code
     */
    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            return "USD";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
