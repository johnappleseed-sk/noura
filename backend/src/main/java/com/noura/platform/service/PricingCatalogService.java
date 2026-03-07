package com.noura.platform.service;

import com.noura.platform.dto.pricing.*;

import java.util.List;
import java.util.UUID;

public interface PricingCatalogService {
    /**
     * Creates price list.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    PriceListDto createPriceList(PriceListRequest request);

    /**
     * Retrieves price lists.
     *
     * @return A list of matching items.
     */
    List<PriceListDto> priceLists();

    /**
     * Upserts price.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    PriceDto upsertPrice(PriceUpsertRequest request);

    /**
     * Quotes variant price.
     *
     * @param variantId The variant id used to locate the target record.
     * @param customerGroupId The customer group id used to locate the target record.
     * @param channelId The channel id used to locate the target record.
     * @return The mapped DTO representation.
     */
    PriceQuoteDto quoteVariantPrice(UUID variantId, UUID customerGroupId, UUID channelId);

    /**
     * Creates promotion.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    PromotionDto createPromotion(PromotionCreateRequest request);

    /**
     * Retrieves active promotions.
     *
     * @return A list of matching items.
     */
    List<PromotionDto> activePromotions();
}
