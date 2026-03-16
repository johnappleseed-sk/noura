package com.noura.platform.service;

import com.noura.platform.dto.merchandising.MerchandisingBoostDto;
import com.noura.platform.dto.merchandising.MerchandisingBoostRequest;
import com.noura.platform.dto.merchandising.MerchandisingPreviewDto;
import com.noura.platform.dto.merchandising.MerchandisingSettingsDto;
import com.noura.platform.dto.merchandising.MerchandisingSettingsUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface MerchandisingAdminService {
    MerchandisingSettingsDto getSettings();

    MerchandisingSettingsDto updateSettings(MerchandisingSettingsUpdateRequest request, String actor);

    List<MerchandisingBoostDto> listBoosts();

    MerchandisingBoostDto createBoost(MerchandisingBoostRequest request, String actor);

    MerchandisingBoostDto updateBoost(UUID boostId, MerchandisingBoostRequest request);

    void deleteBoost(UUID boostId);

    MerchandisingPreviewDto preview(String query, UUID categoryId, UUID storeId, int limit);
}
