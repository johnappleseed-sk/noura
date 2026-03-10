package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.LocationSource;
import com.noura.platform.domain.enums.PhotoPrivacyLevel;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "photo_location_metadata")
public class PhotoLocationMetadata extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID mediaId;

    private UUID ownerId;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    private Instant capturedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LocationSource source = LocationSource.PHOTO_EXIF;

    private Integer accuracyMeters;

    @Column(length = 1024)
    private String addressSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PhotoPrivacyLevel privacyLevel = PhotoPrivacyLevel.INTERNAL;

    @Column(nullable = false)
    private boolean visibleToAdmin;
}

