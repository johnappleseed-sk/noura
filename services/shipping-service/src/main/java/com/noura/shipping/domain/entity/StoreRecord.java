package com.noura.shipping.domain.entity;

import com.noura.shipping.domain.enums.StoreServiceType;
import com.noura.shipping.domain.enums.StoreStatus;
import com.noura.shipping.domain.enums.StoreType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Store compatibility record used by admin-web and service-area assignment flows.
 */
@Getter
@Setter
@Entity
@Table(name = "store_records")
public class StoreRecord extends AuditableEntity {

    @Column(name = "store_code", nullable = false, unique = true, length = 64)
    private String storeCode;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 180)
    private String slug;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StoreType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StoreStatus status;

    @Column(name = "country_code", length = 16)
    private String countryCode;

    @Column(length = 120)
    private String city;

    @Column(name = "address_line_1", length = 240)
    private String addressLine1;

    @Column(name = "address_line_2", length = 240)
    private String addressLine2;

    @Column(name = "postal_code", length = 32)
    private String postalCode;

    @Column(name = "contact_email", length = 180)
    private String contactEmail;

    @Column(name = "contact_phone", length = 64)
    private String contactPhone;

    @Column(precision = 12, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 12, scale = 8)
    private BigDecimal longitude;

    @Column(name = "open_now", nullable = false)
    private boolean openNow = true;

    @Column(name = "preferred_store", nullable = false)
    private boolean preferredStore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supported_services_json", columnDefinition = "jsonb")
    private List<StoreServiceType> supportedServices = new ArrayList<>();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
