package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.PromotionType;
import jakarta.persistence.*;
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
@Table(name = "promotions")
public class Promotion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String code;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionType type;

    @Column(name = "coupon_code")
    private String couponCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions_json", columnDefinition = "json", nullable = false)
    private Map<String, Object> conditions = new LinkedHashMap<>();

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_stackable", nullable = false)
    private boolean stackable = true;

    @Column(nullable = false)
    private int priority = 0;

    @Column(name = "usage_limit_total")
    private Integer usageLimitTotal;

    @Column(name = "usage_limit_per_customer")
    private Integer usageLimitPerCustomer;

    @Column(name = "usage_count", nullable = false)
    private int usageCount = 0;

    @Column(name = "customer_segment", length = 120)
    private String customerSegment;

    @Column(name = "is_archived", nullable = false)
    private boolean archived = false;
}
