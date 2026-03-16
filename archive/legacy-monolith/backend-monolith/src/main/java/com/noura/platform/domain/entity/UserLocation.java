package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.LocationSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_locations")
public class UserLocation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    private Integer accuracyMeters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LocationSource source = LocationSource.BROWSER;

    @Column(length = 1024)
    private String formattedAddress;

    @Column(length = 120)
    private String country;

    @Column(length = 120)
    private String region;

    @Column(length = 120)
    private String city;

    @Column(length = 120)
    private String district;

    @Column(length = 40)
    private String postalCode;

    @Column(length = 220)
    private String placeId;

    @Column(nullable = false)
    private Instant capturedAt = Instant.now();

    @Column(nullable = false)
    private boolean consentGiven;

    @Column(length = 80)
    private String purpose;

    @Column(nullable = false)
    private boolean verified = true;
}

