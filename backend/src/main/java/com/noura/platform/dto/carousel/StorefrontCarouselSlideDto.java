package com.noura.platform.dto.carousel;

import com.noura.platform.domain.enums.CarouselLinkType;

import java.util.UUID;

public record StorefrontCarouselSlideDto(
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
        String backgroundStyle,
        String themeMetadataJson,
        String locale,
        Integer priority,
        Integer position,
        String analyticsKey,
        String experimentKey,
        String audienceSegment,
        String targetingRulesJson
) {
}
