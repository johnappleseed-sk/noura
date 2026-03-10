package com.noura.platform.repository;

import com.noura.platform.domain.entity.AnalyticsEventRecord;
import com.noura.platform.domain.enums.AnalyticsEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalyticsEventRecordRepository extends JpaRepository<AnalyticsEventRecord, UUID> {
    List<AnalyticsEventRecord> findTop50ByOrderByOccurredAtDesc();

    List<AnalyticsEventRecord> findAllByEventTypeInAndOccurredAtBetween(
            List<AnalyticsEventType> eventTypes,
            Instant from,
            Instant to
    );
}
