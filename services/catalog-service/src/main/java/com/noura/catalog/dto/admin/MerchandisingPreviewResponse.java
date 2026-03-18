package com.noura.catalog.dto.admin;

import java.util.List;
import java.util.UUID;

/**
 * Full merchandising preview payload used by admin-web.
 */
public record MerchandisingPreviewResponse(
        MerchandisingSettingsResponse settings,
        UUID categoryId,
        UUID storeId,
        String query,
        List<MerchandisingProductDto> featured,
        List<MerchandisingProductDto> popularity,
        List<MerchandisingProductDto> trending,
        List<MerchandisingProductDto> bestSelling,
        List<MerchandisingProductDto> newest,
        List<MerchandisingBoostResponse> activeBoosts
) {
}
