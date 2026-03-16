package com.noura.platform.service.impl;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.MerchandisingBoost;
import com.noura.platform.domain.entity.MerchandisingSettings;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.dto.merchandising.MerchandisingBoostDto;
import com.noura.platform.dto.merchandising.MerchandisingBoostRequest;
import com.noura.platform.dto.merchandising.MerchandisingPreviewDto;
import com.noura.platform.dto.merchandising.MerchandisingSettingsDto;
import com.noura.platform.dto.merchandising.MerchandisingSettingsUpdateRequest;
import com.noura.platform.repository.MerchandisingBoostRepository;
import com.noura.platform.repository.MerchandisingSettingsRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.service.MerchandisingAdminService;
import com.noura.platform.service.MerchandisingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchandisingAdminServiceImpl implements MerchandisingAdminService {

    private final MerchandisingSettingsRepository merchandisingSettingsRepository;
    private final MerchandisingBoostRepository merchandisingBoostRepository;
    private final ProductRepository productRepository;
    private final MerchandisingService merchandisingService;

    @Override
    @Transactional(readOnly = true)
    public MerchandisingSettingsDto getSettings() {
        return toSettingsDto(currentSettings());
    }

    @Override
    @Transactional
    public MerchandisingSettingsDto updateSettings(MerchandisingSettingsUpdateRequest request, String actor) {
        MerchandisingSettings settings = currentSettingsEntity();
        settings.setPopularityWeight(request.popularityWeight());
        settings.setInventoryWeight(request.inventoryWeight());
        settings.setImpressionWeight(request.impressionWeight());
        settings.setClickWeight(request.clickWeight());
        settings.setClickThroughRateWeight(request.clickThroughRateWeight());
        settings.setManualBoostWeight(request.manualBoostWeight());
        settings.setNewArrivalWindowDays(request.newArrivalWindowDays());
        settings.setNewArrivalBoost(request.newArrivalBoost());
        settings.setTrendingBoost(request.trendingBoost());
        settings.setBestSellerBoost(request.bestSellerBoost());
        settings.setLowStockPenalty(request.lowStockPenalty());
        settings.setMaxPageSize(request.maxPageSize());
        if (actor != null && !actor.isBlank() && (settings.getCreatedBy() == null || settings.getCreatedBy().isBlank())) {
            settings.setCreatedBy(actor.trim());
        }
        return toSettingsDto(merchandisingSettingsRepository.save(settings));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MerchandisingBoostDto> listBoosts() {
        return merchandisingBoostRepository.findAll().stream()
                .sorted(Comparator.comparing(MerchandisingBoost::isActive).reversed().thenComparing(MerchandisingBoost::getLabel, String.CASE_INSENSITIVE_ORDER))
                .map(this::toBoostDto)
                .toList();
    }

    @Override
    @Transactional
    public MerchandisingBoostDto createBoost(MerchandisingBoostRequest request, String actor) {
        MerchandisingBoost boost = new MerchandisingBoost();
        applyBoost(boost, request, actor);
        return toBoostDto(merchandisingBoostRepository.save(boost));
    }

    @Override
    @Transactional
    public MerchandisingBoostDto updateBoost(UUID boostId, MerchandisingBoostRequest request) {
        MerchandisingBoost boost = merchandisingBoostRepository.findById(boostId)
                .orElseThrow(() -> new NotFoundException("MERCHANDISING_BOOST_NOT_FOUND", "Merchandising boost not found."));
        applyBoost(boost, request, null);
        return toBoostDto(merchandisingBoostRepository.save(boost));
    }

    @Override
    @Transactional
    public void deleteBoost(UUID boostId) {
        if (!merchandisingBoostRepository.existsById(boostId)) {
            throw new NotFoundException("MERCHANDISING_BOOST_NOT_FOUND", "Merchandising boost not found.");
        }
        merchandisingBoostRepository.deleteById(boostId);
    }

    @Override
    @Transactional(readOnly = true)
    public MerchandisingPreviewDto preview(String query, UUID categoryId, UUID storeId, int limit) {
        return merchandisingService.preview(query, categoryId, storeId, limit);
    }

    private void applyBoost(MerchandisingBoost boost, MerchandisingBoostRequest request, String actor) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found."));
        if (request.endAt() != null && request.startAt() != null && request.endAt().isBefore(request.startAt())) {
            throw new IllegalArgumentException("Boost endAt must be after startAt.");
        }
        boost.setProduct(product);
        boost.setLabel(request.label().trim());
        boost.setBoostValue(request.boostValue());
        boost.setActive(request.active() == null || request.active());
        boost.setStartAt(request.startAt());
        boost.setEndAt(request.endAt());
        if (actor != null && !actor.isBlank() && (boost.getCreatedBy() == null || boost.getCreatedBy().isBlank())) {
            boost.setCreatedBy(actor.trim());
        }
    }

    private MerchandisingSettings currentSettings() {
        return merchandisingSettingsRepository.findAll().stream().findFirst().orElseGet(MerchandisingSettings::new);
    }

    private MerchandisingSettings currentSettingsEntity() {
        return merchandisingSettingsRepository.findAll().stream().findFirst().orElseGet(MerchandisingSettings::new);
    }

    private MerchandisingSettingsDto toSettingsDto(MerchandisingSettings settings) {
        return new MerchandisingSettingsDto(
                settings.getId(),
                settings.getPopularityWeight(),
                settings.getInventoryWeight(),
                settings.getImpressionWeight(),
                settings.getClickWeight(),
                settings.getClickThroughRateWeight(),
                settings.getManualBoostWeight(),
                settings.getNewArrivalWindowDays(),
                settings.getNewArrivalBoost(),
                settings.getTrendingBoost(),
                settings.getBestSellerBoost(),
                settings.getLowStockPenalty(),
                settings.getMaxPageSize()
        );
    }

    private MerchandisingBoostDto toBoostDto(MerchandisingBoost boost) {
        return new MerchandisingBoostDto(
                boost.getId(),
                boost.getProduct().getId(),
                boost.getProduct().getName(),
                boost.getLabel(),
                boost.getBoostValue(),
                boost.isActive(),
                boost.getStartAt(),
                boost.getEndAt()
        );
    }
}
