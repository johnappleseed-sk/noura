package com.noura.promotion.domain.entity;

import com.noura.promotion.domain.enums.PromotionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;

/**
 * Promotion aggregate root representing one discount or coupon rule.
 */
@Getter
@Setter
@Entity
@Table(name = "promotions")
public class PromotionRecord extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "code", length = 120)
    private String code;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private PromotionType type;

    @Column(name = "coupon_code", length = 120)
    private String couponCode;

    @Column(name = "conditions_json", nullable = false)
    private String conditionsJson;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_stackable", nullable = false)
    private boolean stackable = true;

    @Column(name = "priority", nullable = false)
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

    /**
     * Normalizes mutable fields before insert/update.
     */
    @PrePersist
    @PreUpdate
    protected void normalize() {
        name = trimToNull(name);
        code = normalizeCode(code);
        description = trimToNull(description);
        couponCode = normalizeCode(couponCode);
        conditionsJson = trimToNull(conditionsJson);
        customerSegment = normalizeToken(customerSegment);
        if (conditionsJson == null) {
            conditionsJson = "{}";
        }
        if (priority < 0) {
            priority = 0;
        }
        if (usageCount < 0) {
            usageCount = 0;
        }
        if (usageLimitTotal != null && usageLimitTotal < 0) {
            usageLimitTotal = 0;
        }
        if (usageLimitPerCustomer != null && usageLimitPerCustomer < 0) {
            usageLimitPerCustomer = 0;
        }
    }

    /**
     * Trims source text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Normalizes codes as uppercase.
     *
     * @param value source code
     * @return normalized code
     */
    private String normalizeCode(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * Normalizes free-form tokens as lowercase for equality matching.
     *
     * @param value source token
     * @return normalized token
     */
    private String normalizeToken(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
