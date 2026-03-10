package com.noura.platform.service.impl;

import com.noura.platform.domain.entity.AnalyticsEventRecord;
import com.noura.platform.domain.enums.AnalyticsEventType;
import com.noura.platform.dto.analytics.AnalyticsOverviewDto;
import com.noura.platform.repository.AnalyticsEventRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventServiceImplTest {

    @Mock
    private AnalyticsEventRecordRepository analyticsEventRecordRepository;

    @InjectMocks
    private AnalyticsEventServiceImpl service;

    @Test
    void aggregatesConversionAndAverageOrderValue() {
        Instant now = Instant.now();

        AnalyticsEventRecord productView = event(AnalyticsEventType.PRODUCT_VIEW, now.minusSeconds(60));
        AnalyticsEventRecord checkoutStarted = event(AnalyticsEventType.CHECKOUT_STARTED, now.minusSeconds(30));
        AnalyticsEventRecord checkoutCompleted = event(AnalyticsEventType.CHECKOUT_COMPLETED, now.minusSeconds(10));
        checkoutCompleted.setMetadata(new LinkedHashMap<>() {{
            put("orderTotal", 125.50);
        }});

        when(analyticsEventRecordRepository.findAll()).thenReturn(List.of(productView, checkoutStarted, checkoutCompleted));
        when(analyticsEventRecordRepository.findTop50ByOrderByOccurredAtDesc()).thenReturn(List.of(checkoutCompleted, checkoutStarted, productView));

        AnalyticsOverviewDto overview = service.overview(now.minusSeconds(300), now.plusSeconds(1));

        assertThat(overview.totalEvents()).isEqualTo(3);
        assertThat(overview.productViews()).isEqualTo(1);
        assertThat(overview.checkoutStartedCount()).isEqualTo(1);
        assertThat(overview.checkoutCompletedCount()).isEqualTo(1);
        assertThat(overview.conversionRate()).isEqualByComparingTo("100.00");
        assertThat(overview.averageOrderValue()).isEqualByComparingTo("125.50");
    }

    private AnalyticsEventRecord event(AnalyticsEventType type, Instant occurredAt) {
        AnalyticsEventRecord event = new AnalyticsEventRecord();
        event.setId(UUID.randomUUID());
        event.setEventType(type);
        event.setOccurredAt(occurredAt);
        event.setMetadata(new LinkedHashMap<>());
        return event;
    }
}
