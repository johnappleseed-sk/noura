package com.noura.platform.dto.carousel;

import com.noura.platform.domain.enums.CarouselLinkType;
import com.noura.platform.domain.enums.CarouselStatus;
import com.noura.platform.domain.enums.CarouselVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CarouselSlideRequest(
        @NotBlank(message = "title is required")
        @Size(max = 180, message = "title length must be <= 180")
        String title,

        @Size(max = 190, message = "slug length must be <= 190")
        String slug,

        @Size(max = 2000, message = "description length must be <= 2000")
        String description,

        @NotBlank(message = "imageDesktop is required")
        @Size(max = 2048, message = "imageDesktop length must be <= 2048")
        String imageDesktop,

        @Size(max = 2048, message = "imageMobile length must be <= 2048")
        String imageMobile,

        @Size(max = 255, message = "altText length must be <= 255")
        String altText,

        CarouselLinkType linkType,

        @Size(max = 2048, message = "linkValue length must be <= 2048")
        String linkValue,

        Boolean openInNewTab,

        @Size(max = 120, message = "buttonText length must be <= 120")
        String buttonText,

        @Size(max = 120, message = "secondaryButtonText length must be <= 120")
        String secondaryButtonText,

        CarouselLinkType secondaryLinkType,

        @Size(max = 2048, message = "secondaryLinkValue length must be <= 2048")
        String secondaryLinkValue,

        Boolean secondaryOpenInNewTab,

        Integer position,

        CarouselStatus status,

        CarouselVisibility visibility,

        Instant startAt,

        Instant endAt,

        @Size(max = 80, message = "audienceSegment length must be <= 80")
        String audienceSegment,

        String targetingRulesJson,

        UUID storeId,

        @Size(max = 80, message = "channelId length must be <= 80")
        String channelId,

        @Size(max = 16, message = "locale length must be <= 16")
        String locale,

        @Max(value = 10000, message = "priority must be <= 10000")
        Integer priority,

        @Size(max = 40, message = "backgroundStyle length must be <= 40")
        String backgroundStyle,

        String themeMetadataJson,

        Boolean published,

        Boolean pinned,

        @Size(max = 120, message = "analyticsKey length must be <= 120")
        String analyticsKey,

        @Size(max = 120, message = "experimentKey length must be <= 120")
        String experimentKey
) {
}
