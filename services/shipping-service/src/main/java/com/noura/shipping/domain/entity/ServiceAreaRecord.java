package com.noura.shipping.domain.entity;

import com.noura.shipping.domain.enums.ServiceAreaStatus;
import com.noura.shipping.domain.enums.ServiceAreaType;
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
 * Service-area compatibility record used by admin-web delivery coverage pages.
 */
@Getter
@Setter
@Entity
@Table(name = "service_area_records")
public class ServiceAreaRecord extends AuditableEntity {

    @Column(nullable = false, length = 180)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ServiceAreaType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ServiceAreaStatus status;

    @Column(name = "center_latitude", precision = 12, scale = 8)
    private BigDecimal centerLatitude;

    @Column(name = "center_longitude", precision = 12, scale = 8)
    private BigDecimal centerLongitude;

    @Column(name = "radius_meters")
    private Integer radiusMeters;

    @Column(name = "polygon_geo_json", columnDefinition = "text")
    private String polygonGeoJson;

    @Column(name = "rules_json", columnDefinition = "text")
    private String rulesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "store_ids_json", columnDefinition = "jsonb")
    private List<UUID> storeIds = new ArrayList<>();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
