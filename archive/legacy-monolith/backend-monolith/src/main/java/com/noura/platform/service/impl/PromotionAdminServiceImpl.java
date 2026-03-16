package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Promotion;
import com.noura.platform.domain.entity.PromotionApplication;
import com.noura.platform.dto.pricing.PromotionApplicationItemDto;
import com.noura.platform.dto.pricing.PromotionDto;
import com.noura.platform.dto.pricing.PromotionEvaluationDto;
import com.noura.platform.dto.pricing.PromotionEvaluationRequest;
import com.noura.platform.dto.pricing.PromotionUpdateRequest;
import com.noura.platform.repository.PromotionApplicationRepository;
import com.noura.platform.repository.PromotionRepository;
import com.noura.platform.service.PromotionAdminService;
import com.noura.platform.service.PromotionRuleEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionAdminServiceImpl implements PromotionAdminService {

    private final PromotionRepository promotionRepository;
    private final PromotionApplicationRepository promotionApplicationRepository;
    private final PromotionRuleEngineService promotionRuleEngineService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<PromotionDto> listPromotions(String query, Boolean active, Boolean archived) {
        String normalizedQuery = query == null ? null : query.trim().toLowerCase(Locale.ROOT);
        return promotionRepository.findAll().stream()
                .filter(promotion -> normalizedQuery == null
                        || promotion.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || (promotion.getCode() != null && promotion.getCode().toLowerCase(Locale.ROOT).contains(normalizedQuery)))
                .filter(promotion -> active == null || promotion.isActive() == active)
                .filter(promotion -> archived == null || promotion.isArchived() == archived)
                .sorted((left, right) -> Integer.compare(right.getPriority(), left.getPriority()))
                .map(this::toDto)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PromotionDto getPromotion(UUID promotionId) {
        return toDto(requirePromotion(promotionId));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public PromotionDto updatePromotion(UUID promotionId, PromotionUpdateRequest request) {
        validateWindow(request.startDate(), request.endDate());
        Promotion promotion = requirePromotion(promotionId);
        String normalizedCode = trimToNull(request.code());
        if (normalizedCode != null) {
            promotionRepository.findByCodeIgnoreCase(normalizedCode)
                    .filter(existing -> !existing.getId().equals(promotionId))
                    .ifPresent(existing -> {
                        throw new BadRequestException("PROMOTION_CODE_EXISTS", "Promotion code already exists.");
                    });
        }
        applyRequest(promotion, request);
        Promotion saved = promotionRepository.save(promotion);
        promotionApplicationRepository.deleteAll(promotionApplicationRepository.findByPromotionId(saved.getId()));
        persistApplications(saved, request.applications());
        return toDto(saved);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PromotionEvaluationDto evaluatePromotions(PromotionEvaluationRequest request) {
        return promotionRuleEngineService.evaluate(
                request.subtotal(),
                request.couponCode(),
                request.customerSegment(),
                request.items()
        );
    }

    private Promotion requirePromotion(UUID promotionId) {
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new NotFoundException("PROMOTION_NOT_FOUND", "Promotion not found"));
    }

    private void applyRequest(Promotion promotion, PromotionUpdateRequest request) {
        promotion.setName(request.name().trim());
        promotion.setType(request.type());
        promotion.setCode(trimToNull(request.code()));
        promotion.setDescription(trimToNull(request.description()));
        promotion.setCouponCode(trimToNull(request.couponCode()));
        promotion.setConditions(request.conditions() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.conditions()));
        promotion.setStartDate(request.startDate());
        promotion.setEndDate(request.endDate());
        promotion.setActive(request.active() == null || request.active());
        promotion.setStackable(request.stackable() == null || request.stackable());
        promotion.setPriority(request.priority() == null ? 0 : request.priority());
        promotion.setUsageLimitTotal(request.usageLimitTotal());
        promotion.setUsageLimitPerCustomer(request.usageLimitPerCustomer());
        promotion.setCustomerSegment(trimToNull(request.customerSegment()));
        promotion.setArchived(request.archived() != null && request.archived());
    }

    private void persistApplications(Promotion promotion, List<com.noura.platform.dto.pricing.PromotionApplicationItemRequest> items) {
        if (items == null) {
            return;
        }
        for (var item : items) {
            PromotionApplication application = new PromotionApplication();
            application.setPromotion(promotion);
            application.setApplicableEntityType(item.applicableEntityType());
            application.setApplicableEntityId(item.applicableEntityId());
            promotionApplicationRepository.save(application);
        }
    }

    private PromotionDto toDto(Promotion promotion) {
        List<PromotionApplicationItemDto> applications = promotionApplicationRepository.findByPromotionId(promotion.getId()).stream()
                .map(item -> new PromotionApplicationItemDto(item.getApplicableEntityType(), item.getApplicableEntityId()))
                .toList();
        BigDecimal discountPercent = decimal(promotion.getConditions().get("percent"));
        BigDecimal discountAmount = decimal(promotion.getConditions().get("amount"));
        if (promotion.getType().name().contains("BUNDLE") && discountAmount.signum() == 0) {
            discountAmount = decimal(promotion.getConditions().get("bundleAmount"));
        }
        return new PromotionDto(
                promotion.getId(),
                promotion.getName(),
                promotion.getCode(),
                promotion.getDescription(),
                promotion.getType(),
                promotion.getCouponCode(),
                promotion.getConditions(),
                promotion.getStartDate(),
                promotion.getEndDate(),
                promotion.isActive(),
                promotion.isStackable(),
                promotion.getPriority(),
                promotion.getUsageLimitTotal(),
                promotion.getUsageLimitPerCustomer(),
                promotion.getUsageCount(),
                promotion.getCustomerSegment(),
                promotion.isArchived(),
                discountPercent.signum() > 0 ? discountPercent : null,
                discountAmount.signum() > 0 ? discountAmount : null,
                applications
        );
    }

    private BigDecimal decimal(Object raw) {
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (raw instanceof String string && !string.isBlank()) {
            try {
                return new BigDecimal(string.trim());
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private void validateWindow(Instant startDate, Instant endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException("PROMOTION_WINDOW_INVALID", "Promotion endDate must be after startDate.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
