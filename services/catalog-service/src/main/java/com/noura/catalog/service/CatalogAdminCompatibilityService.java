package com.noura.catalog.service;

import com.noura.catalog.dto.admin.MerchandisingBoostRequest;
import com.noura.catalog.dto.admin.MerchandisingBoostResponse;
import com.noura.catalog.dto.admin.MerchandisingPreviewResponse;
import com.noura.catalog.dto.admin.MerchandisingSettingsResponse;
import com.noura.catalog.dto.admin.MerchandisingSettingsUpdateRequest;
import com.noura.catalog.dto.admin.RecommendationAdminPreviewResponse;
import com.noura.catalog.dto.admin.RecommendationSettingsResponse;
import com.noura.catalog.dto.admin.RecommendationSettingsUpdateRequest;

import java.util.List;
import java.util.UUID;

/**
 * Transitional admin compatibility surface for recommendation and merchandising controls.
 *
 * <p>The extracted platform does not yet run a dedicated admin controls service, so this
 * contract keeps the current admin-web experience stable while staying explicitly scoped to
 * page compatibility and deterministic previews.</p>
 */
public interface CatalogAdminCompatibilityService {

    RecommendationSettingsResponse getRecommendationSettings();

    RecommendationSettingsResponse updateRecommendationSettings(RecommendationSettingsUpdateRequest request, String actorUserId);

    RecommendationAdminPreviewResponse previewRecommendations(String customerRef, UUID productId, int limit);

    MerchandisingSettingsResponse getMerchandisingSettings();

    MerchandisingSettingsResponse updateMerchandisingSettings(MerchandisingSettingsUpdateRequest request, String actorUserId);

    List<MerchandisingBoostResponse> listMerchandisingBoosts();

    MerchandisingBoostResponse createMerchandisingBoost(MerchandisingBoostRequest request, String actorUserId);

    MerchandisingBoostResponse updateMerchandisingBoost(UUID boostId, MerchandisingBoostRequest request, String actorUserId);

    void deleteMerchandisingBoost(UUID boostId);

    MerchandisingPreviewResponse previewMerchandising(String query, UUID categoryId, UUID storeId, int limit);
}
