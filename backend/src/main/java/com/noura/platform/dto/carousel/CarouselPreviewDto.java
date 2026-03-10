package com.noura.platform.dto.carousel;

import java.util.List;

public record CarouselPreviewDto(
        StorefrontCarouselSlideDto slide,
        boolean visibleNow,
        List<String> reasons,
        String previewToken
) {
}
