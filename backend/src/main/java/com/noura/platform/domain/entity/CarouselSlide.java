package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.CarouselLinkType;
import com.noura.platform.domain.enums.CarouselStatus;
import com.noura.platform.domain.enums.CarouselVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "carousel_slides",
        indexes = {
                @Index(name = "idx_carousel_status_published", columnList = "status,published"),
                @Index(name = "idx_carousel_scope", columnList = "store_id,channel_id,locale"),
                @Index(name = "idx_carousel_position_priority", columnList = "position,priority"),
                @Index(name = "idx_carousel_schedule", columnList = "start_at,end_at"),
                @Index(name = "idx_carousel_deleted", columnList = "deleted_at")
        }
)
public class CarouselSlide extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, unique = true, length = 190)
    private String slug;

    @Column(length = 2000)
    private String description;

    @Column(name = "image_desktop", nullable = false, length = 2048)
    private String imageDesktop;

    @Column(name = "image_mobile", length = 2048)
    private String imageMobile;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 24)
    private CarouselLinkType linkType = CarouselLinkType.INTERNAL;

    @Column(name = "link_value", length = 2048)
    private String linkValue;

    @Column(name = "open_in_new_tab", nullable = false)
    private boolean openInNewTab;

    @Column(name = "button_text", length = 120)
    private String buttonText;

    @Column(name = "secondary_button_text", length = 120)
    private String secondaryButtonText;

    @Enumerated(EnumType.STRING)
    @Column(name = "secondary_link_type", length = 24)
    private CarouselLinkType secondaryLinkType;

    @Column(name = "secondary_link_value", length = 2048)
    private String secondaryLinkValue;

    @Column(name = "secondary_open_in_new_tab", nullable = false)
    private boolean secondaryOpenInNewTab;

    @Column(nullable = false)
    private Integer position = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarouselStatus status = CarouselStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarouselVisibility visibility = CarouselVisibility.PUBLIC;

    @Column(name = "start_at")
    private java.time.Instant startAt;

    @Column(name = "end_at")
    private java.time.Instant endAt;

    @Column(name = "audience_segment", length = 80)
    private String audienceSegment;

    @Column(name = "targeting_rules_json", columnDefinition = "TEXT")
    private String targetingRulesJson;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "channel_id", length = 80)
    private String channelId;

    @Column(length = 16)
    private String locale;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "background_style", length = 40)
    private String backgroundStyle = "gradient";

    @Column(name = "theme_metadata_json", columnDefinition = "TEXT")
    private String themeMetadataJson;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private java.time.Instant publishedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "deleted_at")
    private java.time.Instant deletedAt;

    @Column(name = "deleted_by", length = 255)
    private String deletedBy;

    @Column(name = "featured_pinned", nullable = false)
    private boolean pinned;

    @Column(name = "preview_token", nullable = false, length = 64)
    private String previewToken;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "analytics_key", length = 120)
    private String analyticsKey;

    @Column(name = "experiment_key", length = 120)
    private String experimentKey;

    @PrePersist
    void prepareDefaults() {
        if (previewToken == null || previewToken.isBlank()) {
            previewToken = UUID.randomUUID().toString();
        }
        if (versionNumber == null || versionNumber < 1) {
            versionNumber = 1;
        }
        if (position == null) {
            position = 0;
        }
        if (priority == null) {
            priority = 0;
        }
        if (backgroundStyle == null || backgroundStyle.isBlank()) {
            backgroundStyle = "gradient";
        }
        if (status == null) {
            status = CarouselStatus.DRAFT;
        }
        if (visibility == null) {
            visibility = CarouselVisibility.PUBLIC;
        }
        if (linkType == null) {
            linkType = CarouselLinkType.INTERNAL;
        }
    }
}
