package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.AnalyticsEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "analytics_events")
public class AnalyticsEventRecord extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private AnalyticsEventType eventType;

    @Column(name = "session_id", length = 120)
    private String sessionId;

    @Column(name = "customer_ref", length = 120)
    private String customerRef;

    @Column(name = "product_id", length = 120)
    private String productId;

    @Column(name = "order_id", length = 120)
    private String orderId;

    @Column(name = "promotion_code", length = 120)
    private String promotionCode;

    @Column(name = "store_id", length = 120)
    private String storeId;

    @Column(name = "channel_id", length = 120)
    private String channelId;

    @Column(length = 32)
    private String locale;

    @Column(name = "page_path", length = 255)
    private String pagePath;

    @Column(length = 80)
    private String source;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "json")
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
