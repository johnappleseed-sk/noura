package com.noura.platform.service;

import com.noura.platform.dto.analytics.AnalyticsEventDto;
import com.noura.platform.dto.analytics.AnalyticsEventRequest;
import com.noura.platform.dto.analytics.AnalyticsOverviewDto;
import com.noura.platform.dto.analytics.RailPerformanceReportDto;

import java.time.Instant;

public interface AnalyticsEventService {
    AnalyticsEventDto track(AnalyticsEventRequest request);

    AnalyticsOverviewDto overview(Instant from, Instant to);

    RailPerformanceReportDto railPerformance(Instant from, Instant to, String listNamePrefix, String pagePath);
}
