package com.noura.platform.dto.carousel;

import com.noura.platform.domain.enums.CarouselLinkType;
import com.noura.platform.domain.enums.CarouselStatus;
import com.noura.platform.domain.enums.CarouselVisibility;

import java.time.Instant;
import java.util.UUID;

public record CarouselSlideDto(
        UUID id,
        String title,
        String slug,
        String description,
        String imageDesktop,
        String imageMobile,
        String altText,
        CarouselLinkType linkType,
        String linkValue,
        boolean openInNewTab,
        String buttonText,
        String secondaryButtonText,
        CarouselLinkType secondaryLinkType,
        String secondaryLinkValue,
        boolean secondaryOpenInNewTab,
        Integer position,
        CarouselStatus status,
        CarouselVisibility visibility,
        Instant startAt,
        Instant endAt,
        String audienceSegment,
        String targetingRulesJson,
        UUID storeId,
        String channelId,
        String locale,
        Integer priority,
        String backgroundStyle,
        String themeMetadataJson,
        boolean published,
        Instant publishedAt,
        boolean pinned,
        Integer versionNumber,
        String analyticsKey,
        String experimentKey,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        String deletedBy,
        String previewToken,
        boolean storefrontVisibleNow
) {
}
