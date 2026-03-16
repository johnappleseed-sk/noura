package com.noura.platform.dto.merchandising;

import java.util.List;
import java.util.UUID;

public record MerchandisingPreviewDto(
        MerchandisingSettingsDto settings,
        UUID categoryId,
        UUID storeId,
        String query,
        List<MerchandisingProductDto> featured,
        List<MerchandisingProductDto> popularity,
        List<MerchandisingProductDto> trending,
        List<MerchandisingProductDto> bestSelling,
        List<MerchandisingProductDto> newest,
        List<MerchandisingBoostDto> activeBoosts
) {
}
