package com.noura.platform.service.impl;

import com.noura.platform.dto.analytics.AnalyticsEventDto;
import com.noura.platform.dto.analytics.AnalyticsEventRequest;
import com.noura.platform.dto.analytics.AnalyticsOverviewDto;
import com.noura.platform.dto.analytics.RailPerformanceDto;
import com.noura.platform.dto.analytics.RailPerformanceReportDto;
import com.noura.platform.domain.entity.AnalyticsEventRecord;
import com.noura.platform.domain.enums.AnalyticsEventType;
import com.noura.platform.repository.AnalyticsEventRecordRepository;
import com.noura.platform.service.AnalyticsEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AnalyticsEventServiceImpl implements AnalyticsEventService {

    private final AnalyticsEventRecordRepository analyticsEventRecordRepository;

    @Override
    @Transactional
    public AnalyticsEventDto track(AnalyticsEventRequest request) {
        AnalyticsEventRecord entity = new AnalyticsEventRecord();
        entity.setEventType(request.eventType());
        entity.setSessionId(trimToNull(request.sessionId()));
        entity.setCustomerRef(trimToNull(request.customerRef()));
        entity.setProductId(trimToNull(request.productId()));
        entity.setOrderId(trimToNull(request.orderId()));
        entity.setPromotionCode(trimToNull(request.promotionCode()));
        entity.setStoreId(trimToNull(request.storeId()));
        entity.setChannelId(trimToNull(request.channelId()));
        entity.setLocale(trimToNull(request.locale()));
        entity.setPagePath(trimToNull(request.pagePath()));
        entity.setSource(trimToNull(request.source()));
        entity.setOccurredAt(request.occurredAt() == null ? Instant.now() : request.occurredAt());
        entity.setMetadata(request.metadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.metadata()));
        return toDto(analyticsEventRecordRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsOverviewDto overview(Instant from, Instant to) {
        Instant effectiveTo = to == null ? Instant.now() : to;
        Instant effectiveFrom = from == null ? effectiveTo.minus(30, ChronoUnit.DAYS) : from;

        List<AnalyticsEventRecord> events = analyticsEventRecordRepository.findAll().stream()
                .filter(event -> !event.getOccurredAt().isBefore(effectiveFrom) && !event.getOccurredAt().isAfter(effectiveTo))
                .toList();

        Map<AnalyticsEventType, Long> counts = new EnumMap<>(AnalyticsEventType.class);
        for (AnalyticsEventType type : AnalyticsEventType.values()) {
            counts.put(type, 0L);
        }
        BigDecimal completedOrderTotal = BigDecimal.ZERO;
        for (AnalyticsEventRecord event : events) {
            counts.put(event.getEventType(), counts.getOrDefault(event.getEventType(), 0L) + 1L);
            if (event.getEventType() == AnalyticsEventType.CHECKOUT_COMPLETED) {
                completedOrderTotal = completedOrderTotal.add(decimalMetadata(event.getMetadata(), "orderTotal"));
            }
        }

        long productViews = counts.getOrDefault(AnalyticsEventType.PRODUCT_VIEW, 0L);
        long addToCartCount = counts.getOrDefault(AnalyticsEventType.ADD_TO_CART, 0L);
        long removeFromCartCount = counts.getOrDefault(AnalyticsEventType.REMOVE_FROM_CART, 0L);
        long checkoutStartedCount = counts.getOrDefault(AnalyticsEventType.CHECKOUT_STARTED, 0L);
        long checkoutCompletedCount = counts.getOrDefault(AnalyticsEventType.CHECKOUT_COMPLETED, 0L);
        long promotionAppliedCount = counts.getOrDefault(AnalyticsEventType.PROMOTION_APPLIED, 0L);

        BigDecimal conversionRate = percentage(checkoutCompletedCount, productViews);
        BigDecimal cartAbandonmentRate = checkoutStartedCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(checkoutStartedCount - checkoutCompletedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(checkoutStartedCount), 2, RoundingMode.HALF_UP);
        BigDecimal averageOrderValue = checkoutCompletedCount == 0
                ? BigDecimal.ZERO
                : completedOrderTotal.divide(BigDecimal.valueOf(checkoutCompletedCount), 2, RoundingMode.HALF_UP);

        Map<String, Long> flatCounts = new LinkedHashMap<>();
        counts.forEach((type, value) -> flatCounts.put(type.name().toLowerCase(Locale.ROOT), value));

        List<AnalyticsEventDto> recentEvents = analyticsEventRecordRepository.findTop50ByOrderByOccurredAtDesc().stream()
                .map(this::toDto)
                .toList();

        return new AnalyticsOverviewDto(
                effectiveFrom,
                effectiveTo,
                events.size(),
                productViews,
                addToCartCount,
                removeFromCartCount,
                checkoutStartedCount,
                checkoutCompletedCount,
                promotionAppliedCount,
                conversionRate,
                cartAbandonmentRate,
                averageOrderValue,
                flatCounts,
                recentEvents
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RailPerformanceReportDto railPerformance(Instant from, Instant to, String listNamePrefix, String pagePath) {
        Instant effectiveTo = to == null ? Instant.now() : to;
        Instant effectiveFrom = from == null ? effectiveTo.minus(30, ChronoUnit.DAYS) : from;
        String effectivePrefix = trimToNull(listNamePrefix);
        String effectivePagePath = trimToNull(pagePath);

        List<AnalyticsEventType> types = List.of(
                AnalyticsEventType.PRODUCT_IMPRESSION,
                AnalyticsEventType.PRODUCT_CLICK,
                AnalyticsEventType.ADD_TO_CART
        );

        List<AnalyticsEventRecord> events = analyticsEventRecordRepository
                .findAllByEventTypeInAndOccurredAtBetween(types, effectiveFrom, effectiveTo);

        Map<String, long[]> counters = new LinkedHashMap<>();
        for (AnalyticsEventRecord event : events) {
            String eventPagePath = trimToNull(event.getPagePath());
            if (effectivePagePath != null && !Objects.equals(effectivePagePath, eventPagePath)) {
                continue;
            }

            String listName = trimToNull(stringMetadata(event.getMetadata(), "listName"));
            if (listName == null) {
                continue;
            }
            if (effectivePrefix != null && !listName.startsWith(effectivePrefix)) {
                continue;
            }

            long[] counts = counters.computeIfAbsent(listName, ignored -> new long[3]);
            if (event.getEventType() == AnalyticsEventType.PRODUCT_IMPRESSION) {
                counts[0] += 1;
            } else if (event.getEventType() == AnalyticsEventType.PRODUCT_CLICK) {
                counts[1] += 1;
            } else if (event.getEventType() == AnalyticsEventType.ADD_TO_CART) {
                counts[2] += 1;
            }
        }

        List<RailPerformanceDto> rails = counters.entrySet().stream()
                .map(entry -> {
                    long impressions = entry.getValue()[0];
                    long clicks = entry.getValue()[1];
                    long addToCart = entry.getValue()[2];
                    return new RailPerformanceDto(
                            entry.getKey(),
                            impressions,
                            clicks,
                            addToCart,
                            percentage(clicks, impressions),
                            percentage(addToCart, clicks),
                            percentage(addToCart, impressions)
                    );
                })
                .sorted((a, b) -> Long.compare(b.impressions(), a.impressions()))
                .toList();

        return new RailPerformanceReportDto(
                effectiveFrom,
                effectiveTo,
                effectivePrefix,
                effectivePagePath,
                rails
        );
    }

    private AnalyticsEventDto toDto(AnalyticsEventRecord entity) {
        return new AnalyticsEventDto(
                entity.getId(),
                entity.getEventType(),
                entity.getSessionId(),
                entity.getCustomerRef(),
                entity.getProductId(),
                entity.getOrderId(),
                entity.getPromotionCode(),
                entity.getStoreId(),
                entity.getChannelId(),
                entity.getLocale(),
                entity.getPagePath(),
                entity.getSource(),
                entity.getOccurredAt(),
                entity.getMetadata()
        );
    }

    private BigDecimal decimalMetadata(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return BigDecimal.ZERO;
        }
        Object raw = metadata.get(key);
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        if (raw instanceof String string && !string.isBlank()) {
            try {
                return new BigDecimal(string.trim()).setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private String stringMetadata(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return null;
        }
        Object raw = metadata.get(key);
        if (raw == null) {
            return null;
        }
        if (raw instanceof String string) {
            return string;
        }
        return String.valueOf(raw);
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
