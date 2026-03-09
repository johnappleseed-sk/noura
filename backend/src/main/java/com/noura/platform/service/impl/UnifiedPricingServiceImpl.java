package com.noura.platform.service.impl;

import com.noura.platform.commerce.dto.VariantApiDtos;
import com.noura.platform.commerce.entity.CustomerGroup;
import com.noura.platform.commerce.entity.SkuSellUnit;
import com.noura.platform.commerce.entity.SkuUnitBarcode;
import com.noura.platform.commerce.entity.SkuUnitTierPrice;
import com.noura.platform.commerce.entity.UnitOfMeasure;
import com.noura.platform.commerce.service.PricingService;
import com.noura.platform.commerce.service.SkuUnitPricingService;
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
import com.noura.platform.service.PricingCatalogService;
import com.noura.platform.service.UnifiedPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnifiedPricingServiceImpl implements UnifiedPricingService {

    private final PricingCatalogService pricingCatalogService;
    private final ObjectProvider<SkuUnitPricingService> skuUnitPricingServiceProvider;
    private final ObjectProvider<PricingService> commercePricingServiceProvider;

    @Override
    public PriceListDto createPriceList(PriceListRequest request) {
        return pricingCatalogService.createPriceList(request);
    }

    @Override
    public List<PriceListDto> priceLists() {
        return pricingCatalogService.priceLists();
    }

    @Override
    public PriceDto upsertPrice(PriceUpsertRequest request) {
        return pricingCatalogService.upsertPrice(request);
    }

    @Override
    public PriceQuoteDto quoteVariantPrice(UUID variantId, UUID customerGroupId, UUID channelId) {
        return pricingCatalogService.quoteVariantPrice(variantId, customerGroupId, channelId);
    }

    @Override
    public PromotionDto createPromotion(PromotionCreateRequest request) {
        return pricingCatalogService.createPromotion(request);
    }

    @Override
    public List<PromotionDto> activePromotions() {
        return pricingCatalogService.activePromotions();
    }

    // ── Commerce SKU pricing ──────────────────────────────────────

    @Override
    public UnitOfMeasureDto createCommerceUnit(VariantApiDtos.UnitCreateRequest request) {
        return toDto(skuUnitPricingService().createUnit(request));
    }

    @Override
    public CustomerGroupDto createCommerceCustomerGroup(VariantApiDtos.CustomerGroupCreateRequest request) {
        return toDto(skuUnitPricingService().createCustomerGroup(request));
    }

    @Override
    public SkuSellUnitDto upsertCommerceSellUnit(Long variantId, VariantApiDtos.SellUnitUpsertRequest request) {
        return toDto(skuUnitPricingService().upsertSellUnit(variantId, request));
    }

    @Override
    public SkuSellUnitDto updateCommerceSellUnit(Long sellUnitId, VariantApiDtos.SellUnitUpsertRequest request) {
        return toDto(skuUnitPricingService().upsertSellUnitById(sellUnitId, request));
    }

    @Override
    public SkuUnitBarcodeDto addCommerceBarcode(Long sellUnitId, VariantApiDtos.BarcodeCreateRequest request) {
        return toDto(skuUnitPricingService().addBarcode(sellUnitId, request));
    }

    @Override
    public List<SkuUnitTierPriceDto> replaceCommerceTierPrices(Long sellUnitId, VariantApiDtos.TierPriceReplaceRequest request) {
        return skuUnitPricingService().replaceTierPrices(sellUnitId, request)
                .stream().map(this::toDto).toList();
    }

    @Override
    public VariantApiDtos.PricingQuoteResponse commercePosQuote(VariantApiDtos.PricingQuoteRequest request) {
        return commercePricingService().quote(request);
    }

    // ── Entity-to-DTO mappers ─────────────────────────────────────

    private UnitOfMeasureDto toDto(UnitOfMeasure e) {
        return new UnitOfMeasureDto(e.getId(), e.getCode(), e.getName(), e.getPrecisionScale(), e.getActive());
    }

    private CustomerGroupDto toDto(CustomerGroup e) {
        return new CustomerGroupDto(e.getId(), e.getCode(), e.getName(), e.getPriority(), e.getActive());
    }

    private SkuSellUnitDto toDto(SkuSellUnit e) {
        return new SkuSellUnitDto(
                e.getId(),
                e.getVariant() != null ? e.getVariant().getId() : null,
                e.getUnit() != null ? e.getUnit().getId() : null,
                e.getUnit() != null ? e.getUnit().getCode() : null,
                e.getConversionToBase(),
                e.getIsBase(),
                e.getBasePrice(),
                e.getEnabled(),
                e.getVersion()
        );
    }

    private SkuUnitBarcodeDto toDto(SkuUnitBarcode e) {
        return new SkuUnitBarcodeDto(
                e.getId(),
                e.getSkuSellUnit() != null ? e.getSkuSellUnit().getId() : null,
                e.getBarcode(),
                e.getIsPrimary(),
                e.getActive()
        );
    }

    private SkuUnitTierPriceDto toDto(SkuUnitTierPrice e) {
        return new SkuUnitTierPriceDto(
                e.getId(),
                e.getSkuSellUnit() != null ? e.getSkuSellUnit().getId() : null,
                e.getCustomerGroup() != null ? e.getCustomerGroup().getId() : null,
                e.getCustomerGroup() != null ? e.getCustomerGroup().getCode() : null,
                e.getMinQty(),
                e.getUnitPrice(),
                e.getCurrencyCode(),
                e.getEffectiveFrom(),
                e.getEffectiveTo(),
                e.getActive()
        );
    }

    private SkuUnitPricingService skuUnitPricingService() {
        SkuUnitPricingService service = skuUnitPricingServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("Commerce SKU pricing service is not active in the current runtime profile.");
        }
        return service;
    }

    private PricingService commercePricingService() {
        PricingService service = commercePricingServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("Commerce pricing service is not active in the current runtime profile.");
        }
        return service;
    }
}
