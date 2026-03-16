package com.noura.platform.service;

import com.noura.platform.commerce.dto.VariantApiDtos;
import com.noura.platform.dto.pricing.CustomerGroupDto;
import com.noura.platform.dto.pricing.PriceDto;
import com.noura.platform.dto.pricing.PriceListDto;
import com.noura.platform.dto.pricing.PriceListRequest;
import com.noura.platform.dto.pricing.PriceQuoteDto;
import com.noura.platform.dto.pricing.PriceUpsertRequest;
import com.noura.platform.dto.pricing.PromotionCreateRequest;
import com.noura.platform.dto.pricing.PromotionDto;
import com.noura.platform.dto.pricing.SkuSellUnitDto;
import com.noura.platform.dto.pricing.SkuUnitBarcodeDto;
import com.noura.platform.dto.pricing.SkuUnitTierPriceDto;
import com.noura.platform.dto.pricing.UnitOfMeasureDto;

import java.util.List;
import java.util.UUID;

public interface UnifiedPricingService {
    PriceListDto createPriceList(PriceListRequest request);

    List<PriceListDto> priceLists();

    PriceDto upsertPrice(PriceUpsertRequest request);

    PriceQuoteDto quoteVariantPrice(UUID variantId, UUID customerGroupId, UUID channelId);

    PromotionDto createPromotion(PromotionCreateRequest request);

    List<PromotionDto> activePromotions();

    // ── Commerce SKU pricing ──────────────────────────────────────────

    UnitOfMeasureDto createCommerceUnit(VariantApiDtos.UnitCreateRequest request);

    CustomerGroupDto createCommerceCustomerGroup(VariantApiDtos.CustomerGroupCreateRequest request);

    SkuSellUnitDto upsertCommerceSellUnit(Long variantId, VariantApiDtos.SellUnitUpsertRequest request);

    SkuSellUnitDto updateCommerceSellUnit(Long sellUnitId, VariantApiDtos.SellUnitUpsertRequest request);

    SkuUnitBarcodeDto addCommerceBarcode(Long sellUnitId, VariantApiDtos.BarcodeCreateRequest request);

    List<SkuUnitTierPriceDto> replaceCommerceTierPrices(Long sellUnitId, VariantApiDtos.TierPriceReplaceRequest request);

    VariantApiDtos.PricingQuoteResponse commercePosQuote(VariantApiDtos.PricingQuoteRequest request);
}
