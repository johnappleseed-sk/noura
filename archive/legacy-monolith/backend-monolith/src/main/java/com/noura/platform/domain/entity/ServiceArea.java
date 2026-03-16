package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.ServiceAreaStatus;
import com.noura.platform.domain.enums.ServiceAreaType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "service_areas")
public class ServiceArea extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ServiceAreaType type = ServiceAreaType.RADIUS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ServiceAreaStatus status = ServiceAreaStatus.ACTIVE;

    @Column(precision = 10, scale = 7)
    private BigDecimal centerLatitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal centerLongitude;

    private Integer radiusMeters;

    @Lob
    @Column(name = "polygon_geo_json")
    private String polygonGeoJson;

    @Lob
    @Column(name = "rules_json")
    private String rulesJson;

    @ManyToMany
    @JoinTable(
            name = "service_area_stores",
            joinColumns = @JoinColumn(name = "service_area_id"),
            inverseJoinColumns = @JoinColumn(name = "store_id")
    )
    private Set<Store> stores = new HashSet<>();
}

