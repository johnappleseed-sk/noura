package com.noura.promotion.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.promotion.domain.entity.PromotionApplicationRecord;
import com.noura.promotion.domain.entity.PromotionRecord;
import com.noura.promotion.domain.enums.PromotionApplicableEntityType;
import com.noura.promotion.domain.enums.PromotionType;
import com.noura.promotion.dto.promotion.PromotionApplicationItemRequest;
import com.noura.promotion.dto.promotion.PromotionApplicationItemResponse;
import com.noura.promotion.dto.promotion.PromotionEvaluationItemRequest;
import com.noura.promotion.dto.promotion.PromotionEvaluationRequest;
import com.noura.promotion.dto.promotion.PromotionEvaluationResponse;
import com.noura.promotion.dto.promotion.PromotionResponse;
import com.noura.promotion.dto.promotion.PromotionUpsertRequest;
import com.noura.promotion.dto.promotion.PromotionValidationRequest;
import com.noura.promotion.dto.promotion.PromotionValidationResponse;
import com.noura.promotion.exception.NotFoundException;
import com.noura.promotion.exception.PromotionOperationException;
import com.noura.promotion.repository.PromotionApplicationRecordRepository;
import com.noura.promotion.repository.PromotionRecordRepository;
import com.noura.promotion.service.PromotionService;
import com.noura.promotion.service.model.PromotionRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Default implementation of {@link PromotionService}.
 *
 * <p>This service keeps the first extraction intentionally deterministic. Promotion state stays in
 * one service, rule conditions are stored as JSON documents, and evaluation follows a priority-ordered
 * pass adapted from the archived monolith rather than introducing a generic external rule engine.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final PromotionRecordRepository promotionRecordRepository;
    private final PromotionApplicationRecordRepository promotionApplicationRecordRepository;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> listActivePromotions() {
        List<PromotionRecord> promotions = loadCurrentlyActivePromotions();
        return toResponses(promotions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PromotionValidationResponse validatePromoCode(PromotionValidationRequest request) {
        String normalizedPromoCode = normalizeCode(request.promoCode());
        PromotionRecord promotion = findPromotionByPromoCode(normalizedPromoCode);
        if (promotion == null) {
            return new PromotionValidationResponse(
                    false,
                    false,
                    "PROMO_CODE_NOT_FOUND",
                    "No promotion matches the supplied promo code",
                    null,
                    null
            );
        }

        Map<UUID, List<PromotionApplicationRecord>> applicationsByPromotionId =
                loadApplicationsByPromotionId(List.of(promotion.getId()));
        EvaluationInput input = buildEvaluationInput(
                request.subtotal(),
                normalizedPromoCode,
                request.customerSegment(),
                request.items()
        );
        PromotionAssessment assessment = assessPromotion(
                promotion,
                applicationsByPromotionId.getOrDefault(promotion.getId(), List.of()),
                input
        );

        PromotionResponse promotionResponse = toResponse(
                promotion,
                applicationsByPromotionId.getOrDefault(promotion.getId(), List.of())
        );

        if (!assessment.eligible()) {
            return new PromotionValidationResponse(
                    true,
                    false,
                    assessment.reasonCode(),
                    assessment.reasonMessage(),
                    promotionResponse,
                    null
            );
        }

        PromotionEvaluationResponse evaluation = evaluateAgainstCandidates(
                loadCurrentlyActivePromotions(),
                input
        );
        return new PromotionValidationResponse(
                true,
                true,
                "PROMOTION_VALID",
                "Promotion is eligible for the supplied cart snapshot",
                promotionResponse,
                evaluation
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PromotionEvaluationResponse evaluateCartDiscount(PromotionEvaluationRequest request) {
        EvaluationInput input = buildEvaluationInput(
                request.subtotal(),
                request.promoCode(),
                request.customerSegment(),
                request.items()
        );
        return evaluateAgainstCandidates(loadCurrentlyActivePromotions(), input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> listPromotions(PromotionRequestContext context, String query, Boolean active, Boolean archived) {
        assertCanManagePromotions(context);
        String normalizedQuery = normalizeSearchQuery(query);
        List<PromotionRecord> promotions = promotionRecordRepository.findAll().stream()
                .filter(promotion -> normalizedQuery == null || matchesQuery(promotion, normalizedQuery))
                .filter(promotion -> active == null || promotion.isActive() == active)
                .filter(promotion -> archived == null || promotion.isArchived() == archived)
                .sorted(promotionComparator())
                .toList();
        return toResponses(promotions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotion(PromotionRequestContext context, UUID promotionId) {
        assertCanManagePromotions(context);
        PromotionRecord promotion = requirePromotion(promotionId);
        return toResponse(
                promotion,
                promotionApplicationRecordRepository.findByPromotionId(promotionId)
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PromotionResponse createPromotion(PromotionRequestContext context, PromotionUpsertRequest request) {
        assertCanManagePromotions(context);
        validateWindow(request.startDate(), request.endDate());

        Map<String, Object> conditions = normalizedConditions(request.conditions());
        validateConditions(request.type(), conditions);
        assertUniqueIdentifierSpace(null, request.code(), request.couponCode());

        PromotionRecord promotion = new PromotionRecord();
        applyRequest(promotion, request, conditions, context.actorId());
        PromotionRecord saved = promotionRecordRepository.save(promotion);
        List<PromotionApplicationRecord> applications = replaceApplications(saved, request.applications(), context.actorId());

        log.info("Created promotion {} ({})", saved.getName(), saved.getId());
        return toResponse(saved, applications);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PromotionResponse updatePromotion(PromotionRequestContext context, UUID promotionId, PromotionUpsertRequest request) {
        assertCanManagePromotions(context);
        validateWindow(request.startDate(), request.endDate());

        PromotionRecord promotion = requirePromotion(promotionId);
        Map<String, Object> conditions = normalizedConditions(request.conditions());
        validateConditions(request.type(), conditions);
        assertUniqueIdentifierSpace(promotionId, request.code(), request.couponCode());

        applyRequest(promotion, request, conditions, context.actorId());
        promotion.setUpdatedBy(context.actorId());
        PromotionRecord saved = promotionRecordRepository.save(promotion);
        List<PromotionApplicationRecord> applications = replaceApplications(saved, request.applications(), context.actorId());

        log.info("Updated promotion {} ({})", saved.getName(), saved.getId());
        return toResponse(saved, applications);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deletePromotion(PromotionRequestContext context, UUID promotionId) {
        assertCanManagePromotions(context);
        PromotionRecord promotion = requirePromotion(promotionId);
        promotionRecordRepository.delete(promotion);
        log.info("Deleted promotion {} ({})", promotion.getName(), promotion.getId());
    }

    /**
     * Evaluates all candidate promotions in priority order.
     *
     * @param promotions candidate promotions
     * @param input evaluation input
     * @return evaluation result
     */
    private PromotionEvaluationResponse evaluateAgainstCandidates(List<PromotionRecord> promotions, EvaluationInput input) {
        Map<UUID, List<PromotionApplicationRecord>> applicationsByPromotionId = loadApplicationsByPromotionId(
                promotions.stream().map(PromotionRecord::getId).toList()
        );

        BigDecimal discountAmount = ZERO;
        boolean freeShipping = false;
        List<UUID> appliedPromotionIds = new ArrayList<>();
        List<String> appliedPromotionCodes = new ArrayList<>();

        for (PromotionRecord promotion : promotions) {
            if (!matchesRequestedPromoCode(promotion, input.promoCode())) {
                continue;
            }

            PromotionAssessment assessment = assessPromotion(
                    promotion,
                    applicationsByPromotionId.getOrDefault(promotion.getId(), List.of()),
                    input
            );
            if (!assessment.eligible()) {
                continue;
            }

            discountAmount = normalizeMoney(discountAmount.add(assessment.discountAmount()).min(input.subtotal()));
            freeShipping = freeShipping || assessment.freeShipping();
            appliedPromotionIds.add(promotion.getId());
            appliedPromotionCodes.add(resolveDisplayCode(promotion));

            // Stackability stays explicit and deterministic: the first non-stackable hit ends the pass.
            if (!promotion.isStackable()) {
                break;
            }
        }

        BigDecimal discountedSubtotal = normalizeMoney(input.subtotal().subtract(discountAmount).max(ZERO));
        return new PromotionEvaluationResponse(
                input.subtotal(),
                discountAmount,
                discountedSubtotal,
                freeShipping,
                List.copyOf(appliedPromotionIds),
                List.copyOf(appliedPromotionCodes)
        );
    }

    /**
     * Assesses whether one promotion can be applied to the supplied cart snapshot.
     *
     * @param promotion promotion being assessed
     * @param applications scope mappings
     * @param input evaluation input
     * @return assessment result
     */
    private PromotionAssessment assessPromotion(
            PromotionRecord promotion,
            List<PromotionApplicationRecord> applications,
            EvaluationInput input
    ) {
        if (promotion.isArchived()) {
            return PromotionAssessment.ineligible("PROMOTION_ARCHIVED", "Promotion is archived");
        }
        if (!promotion.isActive()) {
            return PromotionAssessment.ineligible("PROMOTION_INACTIVE", "Promotion is inactive");
        }
        if (promotion.getStartDate() != null && input.now().isBefore(promotion.getStartDate())) {
            return PromotionAssessment.ineligible("PROMOTION_NOT_STARTED", "Promotion has not started yet");
        }
        if (promotion.getEndDate() != null && input.now().isAfter(promotion.getEndDate())) {
            return PromotionAssessment.ineligible("PROMOTION_EXPIRED", "Promotion has expired");
        }
        if (!usageAvailable(promotion)) {
            return PromotionAssessment.ineligible(
                    "PROMOTION_USAGE_LIMIT_REACHED",
                    "Promotion usage limit has been reached"
            );
        }
        if (!matchesSegment(promotion, input.customerSegment())) {
            return PromotionAssessment.ineligible(
                    "PROMOTION_CUSTOMER_SEGMENT_MISMATCH",
                    "Promotion is restricted to a different customer segment"
            );
        }
        if (!matchesApplications(applications, input)) {
            return PromotionAssessment.ineligible(
                    "PROMOTION_SCOPE_MISMATCH",
                    "Promotion does not apply to the supplied cart items"
            );
        }

        Map<String, Object> conditions = readConditions(promotion.getConditionsJson());
        BigDecimal discountAmount = switch (promotion.getType()) {
            case PERCENTAGE -> percentageDiscount(input.subtotal(), decimalValue(conditions.get("percent")));
            case FIXED -> fixedDiscount(input.subtotal(), decimalValue(conditions.get("amount")));
            case CART_THRESHOLD_DISCOUNT -> thresholdDiscount(input.subtotal(), conditions);
            case BUY_X_GET_Y -> buyXGetYDiscount(input.items(), conditions);
            case PRODUCT_BUNDLE_DISCOUNT -> bundleDiscount(input.subtotal(), input.items(), conditions);
            case FREE_SHIPPING -> ZERO;
        };

        boolean freeShipping = promotion.getType() == PromotionType.FREE_SHIPPING
                && meetsThreshold(conditions, input.subtotal());

        if (discountAmount.signum() <= 0 && !freeShipping) {
            return PromotionAssessment.ineligible(
                    "PROMOTION_CART_NOT_ELIGIBLE",
                    "Promotion conditions are not satisfied for the supplied cart snapshot"
            );
        }

        return new PromotionAssessment(true, "PROMOTION_ELIGIBLE", "Promotion is eligible", discountAmount, freeShipping);
    }

    /**
     * Replaces all scope mappings for one promotion using a deduplicated request payload.
     *
     * @param promotion promotion aggregate
     * @param requests requested scope mappings
     * @param actor actor identifier
     * @return persisted mappings
     */
    private List<PromotionApplicationRecord> replaceApplications(
            PromotionRecord promotion,
            List<PromotionApplicationItemRequest> requests,
            String actor
    ) {
        promotionApplicationRecordRepository.deleteByPromotionId(promotion.getId());
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        Set<String> seen = new LinkedHashSet<>();
        List<PromotionApplicationRecord> persisted = new ArrayList<>();
        for (PromotionApplicationItemRequest request : requests) {
            String dedupeKey = request.applicableEntityType().name() + ":" + request.applicableEntityId();
            if (!seen.add(dedupeKey)) {
                continue;
            }
            PromotionApplicationRecord application = new PromotionApplicationRecord();
            application.setPromotion(promotion);
            application.setApplicableEntityType(request.applicableEntityType());
            application.setApplicableEntityId(request.applicableEntityId());
            application.setCreatedBy(actor);
            application.setUpdatedBy(actor);
            persisted.add(promotionApplicationRecordRepository.save(application));
        }
        return List.copyOf(persisted);
    }

    /**
     * Applies a validated upsert request to a promotion aggregate.
     *
     * @param promotion target promotion
     * @param request request payload
     * @param conditions normalized conditions map
     * @param actor actor identifier
     */
    private void applyRequest(
            PromotionRecord promotion,
            PromotionUpsertRequest request,
            Map<String, Object> conditions,
            String actor
    ) {
        promotion.setName(request.name());
        promotion.setType(request.type());
        promotion.setCode(request.code());
        promotion.setDescription(request.description());
        promotion.setCouponCode(request.couponCode());
        promotion.setConditionsJson(writeConditions(conditions));
        promotion.setStartDate(request.startDate());
        promotion.setEndDate(request.endDate());
        promotion.setActive(request.active() == null || request.active());
        promotion.setStackable(request.stackable() == null || request.stackable());
        promotion.setPriority(request.priority() == null ? 0 : request.priority());
        promotion.setUsageLimitTotal(request.usageLimitTotal());
        promotion.setUsageLimitPerCustomer(request.usageLimitPerCustomer());
        promotion.setCustomerSegment(request.customerSegment());
        promotion.setArchived(request.archived() != null && request.archived());
        if (promotion.getCreatedBy() == null) {
            promotion.setCreatedBy(actor);
        }
        promotion.setUpdatedBy(actor);
    }

    /**
     * Loads currently active, date-valid, non-archived, usage-available promotions.
     *
     * @return active promotion list
     */
    private List<PromotionRecord> loadCurrentlyActivePromotions() {
        Instant now = Instant.now();
        return promotionRecordRepository.findByActiveTrueAndArchivedFalse().stream()
                .filter(promotion -> !promotion.isArchived())
                .filter(promotion -> isWithinWindow(now, promotion.getStartDate(), promotion.getEndDate()))
                .filter(this::usageAvailable)
                .sorted(promotionComparator())
                .toList();
    }

    /**
     * Builds response DTOs for a batch of promotions while avoiding repeated application queries.
     *
     * @param promotions promotions to map
     * @return response DTO list
     */
    private List<PromotionResponse> toResponses(List<PromotionRecord> promotions) {
        Map<UUID, List<PromotionApplicationRecord>> applicationsByPromotionId = loadApplicationsByPromotionId(
                promotions.stream().map(PromotionRecord::getId).toList()
        );
        return promotions.stream()
                .map(promotion -> toResponse(
                        promotion,
                        applicationsByPromotionId.getOrDefault(promotion.getId(), List.of())
                ))
                .toList();
    }

    /**
     * Maps one promotion aggregate to its API response shape.
     *
     * @param promotion promotion aggregate
     * @param applications scope mappings
     * @return response DTO
     */
    private PromotionResponse toResponse(PromotionRecord promotion, List<PromotionApplicationRecord> applications) {
        Map<String, Object> conditions = readConditions(promotion.getConditionsJson());
        return new PromotionResponse(
                promotion.getId(),
                promotion.getName(),
                promotion.getCode(),
                promotion.getDescription(),
                promotion.getType(),
                promotion.getCouponCode(),
                conditions,
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
                resolveDiscountPercent(promotion.getType(), conditions),
                resolveDiscountAmount(promotion.getType(), conditions),
                applications.stream()
                        .map(item -> new PromotionApplicationItemResponse(
                                item.getApplicableEntityType(),
                                item.getApplicableEntityId()
                        ))
                        .toList()
        );
    }

    /**
     * Loads scope mappings for a promotion batch.
     *
     * @param promotionIds promotion identifiers
     * @return mappings grouped by promotion identifier
     */
    private Map<UUID, List<PromotionApplicationRecord>> loadApplicationsByPromotionId(Collection<UUID> promotionIds) {
        if (promotionIds == null || promotionIds.isEmpty()) {
            return Map.of();
        }
        return promotionApplicationRecordRepository.findByPromotionIdIn(promotionIds).stream()
                .collect(LinkedHashMap::new, (map, application) -> map
                        .computeIfAbsent(application.getPromotion().getId(), ignored -> new ArrayList<>())
                        .add(application), Map::putAll);
    }

    /**
     * Resolves one promotion by identifier or throws a 404.
     *
     * @param promotionId promotion identifier
     * @return promotion aggregate
     */
    private PromotionRecord requirePromotion(UUID promotionId) {
        return promotionRecordRepository.findById(promotionId)
                .orElseThrow(() -> new NotFoundException("PROMOTION_NOT_FOUND", "Promotion not found"));
    }

    /**
     * Ensures the actor can manage promotions.
     *
     * @param context request context
     */
    private void assertCanManagePromotions(PromotionRequestContext context) {
        if (context == null || !context.canManageAllPromotions()) {
            throw new PromotionOperationException(
                    HttpStatus.FORBIDDEN,
                    "PROMOTION_FORBIDDEN",
                    "Promotion management requires admin or marketing permissions"
            );
        }
    }

    /**
     * Validates identifier uniqueness across both code and coupon-code namespaces to avoid ambiguous lookups.
     *
     * @param currentPromotionId current promotion identifier when updating
     * @param code requested code
     * @param couponCode requested coupon code
     */
    private void assertUniqueIdentifierSpace(UUID currentPromotionId, String code, String couponCode) {
        String normalizedCode = normalizeCode(code);
        String normalizedCouponCode = normalizeCode(couponCode);
        if (normalizedCode == null && normalizedCouponCode == null) {
            return;
        }

        for (PromotionRecord existing : promotionRecordRepository.findAll()) {
            if (currentPromotionId != null && currentPromotionId.equals(existing.getId())) {
                continue;
            }
            String existingCode = normalizeCode(existing.getCode());
            String existingCouponCode = normalizeCode(existing.getCouponCode());
            if (normalizedCode != null
                    && (normalizedCode.equals(existingCode) || normalizedCode.equals(existingCouponCode))) {
                throw new PromotionOperationException(
                        HttpStatus.CONFLICT,
                        "PROMOTION_CODE_EXISTS",
                        "Promotion code already exists in the promotion identifier space"
                );
            }
            if (normalizedCouponCode != null
                    && (normalizedCouponCode.equals(existingCode) || normalizedCouponCode.equals(existingCouponCode))) {
                throw new PromotionOperationException(
                        HttpStatus.CONFLICT,
                        "PROMOTION_COUPON_CODE_EXISTS",
                        "Promotion coupon code already exists in the promotion identifier space"
                );
            }
        }
    }

    /**
     * Finds one promotion by code or coupon code and rejects ambiguous matches.
     *
     * @param promoCode normalized promo code
     * @return matching promotion or {@code null}
     */
    private PromotionRecord findPromotionByPromoCode(String promoCode) {
        if (promoCode == null) {
            return null;
        }
        List<PromotionRecord> matches = promotionRecordRepository.findAll().stream()
                .filter(promotion -> promoCode.equals(normalizeCode(promotion.getCode()))
                        || promoCode.equals(normalizeCode(promotion.getCouponCode())))
                .toList();
        if (matches.size() > 1) {
            throw new PromotionOperationException(
                    HttpStatus.CONFLICT,
                    "PROMOTION_CODE_AMBIGUOUS",
                    "Multiple promotions share the same promo code namespace"
            );
        }
        return matches.isEmpty() ? null : matches.getFirst();
    }

    /**
     * Builds normalized evaluation input from API request data.
     *
     * @param subtotal requested subtotal override
     * @param promoCode requested promo code
     * @param customerSegment requested customer segment
     * @param items cart items
     * @return normalized evaluation input
     */
    private EvaluationInput buildEvaluationInput(
            BigDecimal subtotal,
            String promoCode,
            String customerSegment,
            List<PromotionEvaluationItemRequest> items
    ) {
        List<PromotionEvaluationItemRequest> safeItems = items == null ? List.of() : List.copyOf(items);
        BigDecimal effectiveSubtotal = subtotal == null ? computeSubtotal(safeItems) : normalizeMoney(subtotal);
        return new EvaluationInput(
                effectiveSubtotal,
                normalizeCode(promoCode),
                normalizeToken(customerSegment),
                safeItems,
                collectIds(safeItems, PromotionEvaluationItemRequest::productId),
                collectIds(safeItems, PromotionEvaluationItemRequest::categoryId),
                collectIds(safeItems, PromotionEvaluationItemRequest::variantId),
                Instant.now()
        );
    }

    /**
     * Computes subtotal from item prices when callers do not provide an explicit subtotal.
     *
     * @param items evaluation items
     * @return computed subtotal
     */
    private BigDecimal computeSubtotal(List<PromotionEvaluationItemRequest> items) {
        return normalizeMoney(items.stream()
                .map(item -> normalizeMoney(item.unitPrice()).multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(ZERO, BigDecimal::add));
    }

    /**
     * Checks application-scope mappings against the cart snapshot.
     *
     * @param applications scope mappings
     * @param input evaluation input
     * @return {@code true} when at least one scope matches or the promotion is global
     */
    private boolean matchesApplications(List<PromotionApplicationRecord> applications, EvaluationInput input) {
        if (applications == null || applications.isEmpty()) {
            return true;
        }
        Map<PromotionApplicableEntityType, Set<UUID>> applicableIds = new LinkedHashMap<>();
        applicableIds.put(PromotionApplicableEntityType.PRODUCT, input.productIds());
        applicableIds.put(PromotionApplicableEntityType.CATEGORY, input.categoryIds());
        applicableIds.put(PromotionApplicableEntityType.VARIANT, input.variantIds());

        for (PromotionApplicationRecord application : applications) {
            if (application.getApplicableEntityType() == PromotionApplicableEntityType.COLLECTION) {
                Map<String, Object> conditions = readConditions(application.getPromotion().getConditionsJson());
                if (collectionMatches(conditions, application.getApplicableEntityId(), input.productIds())) {
                    return true;
                }
                continue;
            }
            Set<UUID> ids = applicableIds.getOrDefault(application.getApplicableEntityType(), Set.of());
            if (ids.contains(application.getApplicableEntityId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves collection matches using deterministic collection-product conditions from archived logic.
     *
     * @param conditions promotion conditions
     * @param collectionId configured collection identifier
     * @param productIds product identifiers in the cart
     * @return {@code true} when the cart intersects the configured collection product set
     */
    private boolean collectionMatches(Map<String, Object> conditions, UUID collectionId, Set<UUID> productIds) {
        Object rawCollectionProducts = conditions.get("collectionProductIds");
        if (!(rawCollectionProducts instanceof List<?> list) || list.isEmpty()) {
            return false;
        }
        Set<String> collectionProducts = list.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        return productIds.stream().map(UUID::toString).anyMatch(collectionProducts::contains)
                || collectionProducts.contains(collectionId.toString());
    }

    /**
     * Validates type-specific condition contracts.
     *
     * @param type promotion type
     * @param conditions normalized conditions
     */
    private void validateConditions(PromotionType type, Map<String, Object> conditions) {
        BigDecimal percent = decimalValue(conditions.get("percent"));
        BigDecimal amount = decimalValue(conditions.get("amount"));
        BigDecimal bundleAmount = decimalValue(conditions.get("bundleAmount"));
        BigDecimal threshold = decimalValue(conditions.get("threshold"));

        switch (type) {
            case PERCENTAGE -> require(percent.signum() > 0 && percent.compareTo(BigDecimal.valueOf(100)) <= 0,
                    "PROMOTION_CONDITIONS_INVALID",
                    "Percentage promotions require a percent value between 0 and 100");
            case FIXED -> require(amount.signum() > 0,
                    "PROMOTION_CONDITIONS_INVALID",
                    "Fixed promotions require a positive amount");
            case CART_THRESHOLD_DISCOUNT -> {
                require(threshold.signum() > 0,
                        "PROMOTION_CONDITIONS_INVALID",
                        "Threshold discount promotions require a positive threshold");
                require(
                        (percent.signum() > 0 && percent.compareTo(BigDecimal.valueOf(100)) <= 0) || amount.signum() > 0,
                        "PROMOTION_CONDITIONS_INVALID",
                        "Threshold discount promotions require either a positive percent or amount"
                );
            }
            case BUY_X_GET_Y -> {
                require(integerValue(conditions.get("buyQty")) > 0,
                        "PROMOTION_CONDITIONS_INVALID",
                        "Buy X Get Y promotions require buyQty to be greater than 0");
                require(integerValue(conditions.get("getQty")) > 0,
                        "PROMOTION_CONDITIONS_INVALID",
                        "Buy X Get Y promotions require getQty to be greater than 0");
                BigDecimal discountPercent = decimalValue(conditions.getOrDefault("discountPercent", 100));
                require(discountPercent.signum() > 0 && discountPercent.compareTo(BigDecimal.valueOf(100)) <= 0,
                        "PROMOTION_CONDITIONS_INVALID",
                        "Buy X Get Y discountPercent must be between 0 and 100");
            }
            case FREE_SHIPPING -> {
                if (conditions.containsKey("threshold")) {
                    require(threshold.signum() > 0,
                            "PROMOTION_CONDITIONS_INVALID",
                            "Free shipping threshold must be greater than 0 when provided");
                }
            }
            case PRODUCT_BUNDLE_DISCOUNT -> require(
                    (bundleAmount.signum() > 0)
                            || (percent.signum() > 0 && percent.compareTo(BigDecimal.valueOf(100)) <= 0),
                    "PROMOTION_CONDITIONS_INVALID",
                    "Bundle promotions require a positive bundleAmount or percent"
            );
        }
    }

    /**
     * Enforces the promotion date window.
     *
     * @param startDate start date
     * @param endDate end date
     */
    private void validateWindow(Instant startDate, Instant endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new PromotionOperationException(
                    HttpStatus.BAD_REQUEST,
                    "PROMOTION_WINDOW_INVALID",
                    "Promotion endDate must be after startDate"
            );
        }
    }

    /**
     * Calculates a percentage discount.
     *
     * @param subtotal eligible subtotal
     * @param percent percentage value
     * @return discount amount
     */
    private BigDecimal percentageDiscount(BigDecimal subtotal, BigDecimal percent) {
        if (percent.signum() <= 0) {
            return ZERO;
        }
        return normalizeMoney(subtotal.multiply(percent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
    }

    /**
     * Calculates a fixed discount amount.
     *
     * @param subtotal eligible subtotal
     * @param amount requested discount amount
     * @return discount amount clipped to subtotal
     */
    private BigDecimal fixedDiscount(BigDecimal subtotal, BigDecimal amount) {
        if (amount.signum() <= 0) {
            return ZERO;
        }
        return normalizeMoney(amount.min(subtotal));
    }

    /**
     * Calculates threshold-based discounts using either percent or amount conditions.
     *
     * @param subtotal eligible subtotal
     * @param conditions promotion conditions
     * @return discount amount
     */
    private BigDecimal thresholdDiscount(BigDecimal subtotal, Map<String, Object> conditions) {
        if (!meetsThreshold(conditions, subtotal)) {
            return ZERO;
        }
        BigDecimal percent = decimalValue(conditions.get("percent"));
        if (percent.signum() > 0) {
            return percentageDiscount(subtotal, percent);
        }
        return fixedDiscount(subtotal, decimalValue(conditions.get("amount")));
    }

    /**
     * Calculates deterministic Buy X Get Y discounts using the cheapest unit price in the cart.
     *
     * @param items cart items
     * @param conditions promotion conditions
     * @return discount amount
     */
    private BigDecimal buyXGetYDiscount(List<PromotionEvaluationItemRequest> items, Map<String, Object> conditions) {
        if (items == null || items.isEmpty()) {
            return ZERO;
        }
        int buyQty = integerValue(conditions.get("buyQty"));
        int getQty = integerValue(conditions.get("getQty"));
        if (buyQty <= 0 || getQty <= 0) {
            return ZERO;
        }
        int totalQty = items.stream().mapToInt(PromotionEvaluationItemRequest::quantity).sum();
        if (totalQty < buyQty + getQty) {
            return ZERO;
        }
        BigDecimal discountPercent = decimalValue(conditions.getOrDefault("discountPercent", 100));
        BigDecimal cheapestUnit = items.stream()
                .map(PromotionEvaluationItemRequest::unitPrice)
                .map(this::normalizeMoney)
                .min(BigDecimal::compareTo)
                .orElse(ZERO);
        int eligibleUnits = totalQty / (buyQty + getQty) * getQty;
        BigDecimal base = cheapestUnit.multiply(BigDecimal.valueOf(eligibleUnits));
        return normalizeMoney(base.multiply(discountPercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
    }

    /**
     * Calculates bundle discounts when the required products are present.
     *
     * @param subtotal subtotal
     * @param items cart items
     * @param conditions promotion conditions
     * @return discount amount
     */
    private BigDecimal bundleDiscount(
            BigDecimal subtotal,
            List<PromotionEvaluationItemRequest> items,
            Map<String, Object> conditions
    ) {
        if (items == null || items.isEmpty()) {
            return ZERO;
        }
        Set<String> requestedProducts = items.stream()
                .map(PromotionEvaluationItemRequest::productId)
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        Set<String> bundleProducts = arrayStringSet(conditions.get("bundleProductIds"));
        if (!bundleProducts.isEmpty() && !requestedProducts.containsAll(bundleProducts)) {
            return ZERO;
        }
        BigDecimal bundleAmount = decimalValue(conditions.get("bundleAmount"));
        if (bundleAmount.signum() > 0) {
            return fixedDiscount(subtotal, bundleAmount);
        }
        return percentageDiscount(subtotal, decimalValue(conditions.get("percent")));
    }

    /**
     * Resolves whether threshold conditions are satisfied.
     *
     * @param conditions promotion conditions
     * @param subtotal subtotal
     * @return {@code true} when threshold is met or absent
     */
    private boolean meetsThreshold(Map<String, Object> conditions, BigDecimal subtotal) {
        BigDecimal threshold = decimalValue(conditions.get("threshold"));
        return threshold.signum() <= 0 || subtotal.compareTo(threshold) >= 0;
    }

    /**
     * Returns whether the promotion usage counter is still below the total limit.
     *
     * @param promotion promotion aggregate
     * @return {@code true} when usage is still available
     */
    private boolean usageAvailable(PromotionRecord promotion) {
        return promotion.getUsageLimitTotal() == null || promotion.getUsageCount() < promotion.getUsageLimitTotal();
    }

    /**
     * Checks customer-segment equality after normalization.
     *
     * @param promotion promotion aggregate
     * @param customerSegment normalized customer segment
     * @return {@code true} when the promotion has no segment restriction or matches exactly
     */
    private boolean matchesSegment(PromotionRecord promotion, String customerSegment) {
        String requiredSegment = normalizeToken(promotion.getCustomerSegment());
        return requiredSegment == null || Objects.equals(requiredSegment, customerSegment);
    }

    /**
     * Applies promo-code filtering while still allowing automatic promotions to participate.
     *
     * @param promotion promotion aggregate
     * @param requestedPromoCode normalized requested promo code
     * @return {@code true} when the promotion should be considered in evaluation
     */
    private boolean matchesRequestedPromoCode(PromotionRecord promotion, String requestedPromoCode) {
        String code = normalizeCode(promotion.getCode());
        String couponCode = normalizeCode(promotion.getCouponCode());
        if (requestedPromoCode == null) {
            return code == null && couponCode == null;
        }
        if (code == null && couponCode == null) {
            return true;
        }
        return requestedPromoCode.equals(code) || requestedPromoCode.equals(couponCode);
    }

    /**
     * Parses stored conditions JSON into a structured map.
     *
     * @param conditionsJson persisted JSON
     * @return structured conditions map
     */
    private Map<String, Object> readConditions(String conditionsJson) {
        String normalized = conditionsJson == null || conditionsJson.isBlank() ? "{}" : conditionsJson.trim();
        try {
            return objectMapper.readValue(normalized, MAP_TYPE);
        } catch (Exception ex) {
            log.warn("Failed to parse promotion conditions JSON, returning empty conditions instead", ex);
            return new LinkedHashMap<>();
        }
    }

    /**
     * Serializes conditions JSON for persistence.
     *
     * @param conditions conditions map
     * @return JSON string
     */
    private String writeConditions(Map<String, Object> conditions) {
        try {
            return objectMapper.writeValueAsString(normalizedConditions(conditions));
        } catch (JsonProcessingException ex) {
            throw new PromotionOperationException(
                    HttpStatus.BAD_REQUEST,
                    "PROMOTION_CONDITIONS_INVALID_JSON",
                    "Promotion conditions could not be serialized"
            );
        }
    }

    /**
     * Normalizes condition maps into mutable deterministic JSON payloads.
     *
     * @param conditions input conditions
     * @return normalized mutable map
     */
    private Map<String, Object> normalizedConditions(Map<String, Object> conditions) {
        return conditions == null ? new LinkedHashMap<>() : new LinkedHashMap<>(conditions);
    }

    /**
     * Creates a comparator shared by admin lists and evaluation ordering.
     *
     * @return promotion comparator
     */
    private Comparator<PromotionRecord> promotionComparator() {
        return Comparator.comparingInt(PromotionRecord::getPriority)
                .reversed()
                .thenComparing(PromotionRecord::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PromotionRecord::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    /**
     * Derives display discount percent from the persisted condition shape.
     *
     * @param type promotion type
     * @param conditions conditions map
     * @return display percent or {@code null}
     */
    private BigDecimal resolveDiscountPercent(PromotionType type, Map<String, Object> conditions) {
        BigDecimal percent = switch (type) {
            case BUY_X_GET_Y -> decimalValue(conditions.getOrDefault("discountPercent", null));
            default -> decimalValue(conditions.get("percent"));
        };
        return percent.signum() > 0 ? normalizeMoney(percent) : null;
    }

    /**
     * Derives display discount amount from the persisted condition shape.
     *
     * @param type promotion type
     * @param conditions conditions map
     * @return display amount or {@code null}
     */
    private BigDecimal resolveDiscountAmount(PromotionType type, Map<String, Object> conditions) {
        BigDecimal amount = decimalValue(conditions.get("amount"));
        if (amount.signum() <= 0 && type == PromotionType.PRODUCT_BUNDLE_DISCOUNT) {
            amount = decimalValue(conditions.get("bundleAmount"));
        }
        return amount.signum() > 0 ? normalizeMoney(amount) : null;
    }

    /**
     * Collects entity identifiers from evaluation items.
     *
     * @param items evaluation items
     * @param extractor identifier extractor
     * @return identifier set
     */
    private Set<UUID> collectIds(
            List<PromotionEvaluationItemRequest> items,
            java.util.function.Function<PromotionEvaluationItemRequest, UUID> extractor
    ) {
        return items.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    /**
     * Converts arrays stored in JSON conditions to a string set.
     *
     * @param raw source value
     * @return string set
     */
    private Set<String> arrayStringSet(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return Set.of();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    /**
     * Normalizes money values to the service-wide scale.
     *
     * @param value source amount
     * @return normalized amount
     */
    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Converts arbitrary condition values to a decimal.
     *
     * @param raw raw value
     * @return decimal value or zero when absent/invalid
     */
    private BigDecimal decimalValue(Object raw) {
        if (raw instanceof BigDecimal decimal) {
            return normalizeMoney(decimal);
        }
        if (raw instanceof Number number) {
            return normalizeMoney(BigDecimal.valueOf(number.doubleValue()));
        }
        if (raw instanceof String string && !string.isBlank()) {
            try {
                return normalizeMoney(new BigDecimal(string.trim()));
            } catch (NumberFormatException ignored) {
                return ZERO;
            }
        }
        return ZERO;
    }

    /**
     * Converts arbitrary condition values to an integer.
     *
     * @param raw raw value
     * @return integer value or zero when absent/invalid
     */
    private int integerValue(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String string && !string.isBlank()) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Returns whether a time window is currently active.
     *
     * @param now current time
     * @param start start time
     * @param end end time
     * @return {@code true} when active
     */
    private boolean isWithinWindow(Instant now, Instant start, Instant end) {
        return (start == null || !now.isBefore(start)) && (end == null || !now.isAfter(end));
    }

    /**
     * Resolves display code for applied-promotion reporting.
     *
     * @param promotion promotion aggregate
     * @return code, coupon code, or fallback name
     */
    private String resolveDisplayCode(PromotionRecord promotion) {
        String code = normalizeNullable(promotion.getCode());
        if (code != null) {
            return code;
        }
        String couponCode = normalizeNullable(promotion.getCouponCode());
        return couponCode != null ? couponCode : promotion.getName();
    }

    /**
     * Evaluates whether a query fragment matches the promotion search fields.
     *
     * @param promotion promotion aggregate
     * @param query normalized query
     * @return {@code true} when matched
     */
    private boolean matchesQuery(PromotionRecord promotion, String query) {
        String normalizedName = normalizeSearchQuery(promotion.getName());
        String normalizedCode = normalizeSearchQuery(promotion.getCode());
        String normalizedCouponCode = normalizeSearchQuery(promotion.getCouponCode());
        return normalizedName != null && normalizedName.contains(query)
                || normalizedCode != null && normalizedCode.contains(query)
                || normalizedCouponCode != null && normalizedCouponCode.contains(query);
    }

    /**
     * Throws a business exception when a condition is not met.
     *
     * @param expression validation expression
     * @param code error code
     * @param message human-readable message
     */
    private void require(boolean expression, String code, String message) {
        if (!expression) {
            throw new PromotionOperationException(HttpStatus.BAD_REQUEST, code, message);
        }
    }

    /**
     * Normalizes free-form code input to uppercase.
     *
     * @param value source value
     * @return normalized code or {@code null}
     */
    private String normalizeCode(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * Normalizes segment/search tokens to lowercase.
     *
     * @param value source value
     * @return normalized token or {@code null}
     */
    private String normalizeToken(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes search fragments to lowercase.
     *
     * @param value source value
     * @return normalized query or {@code null}
     */
    private String normalizeSearchQuery(String value) {
        return normalizeToken(value);
    }

    /**
     * Trims text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text
     */
    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Structured evaluation input.
     *
     * @param subtotal effective subtotal
     * @param promoCode normalized promo code
     * @param customerSegment normalized customer segment
     * @param items cart items
     * @param productIds cart product identifiers
     * @param categoryIds cart category identifiers
     * @param variantIds cart variant identifiers
     * @param now evaluation timestamp
     */
    private record EvaluationInput(
            BigDecimal subtotal,
            String promoCode,
            String customerSegment,
            List<PromotionEvaluationItemRequest> items,
            Set<UUID> productIds,
            Set<UUID> categoryIds,
            Set<UUID> variantIds,
            Instant now
    ) {
    }

    /**
     * Structured eligibility assessment for one promotion.
     *
     * @param eligible whether the promotion is eligible
     * @param reasonCode stable machine-readable reason
     * @param reasonMessage human-readable reason
     * @param discountAmount resolved discount amount
     * @param freeShipping whether shipping becomes free
     */
    private record PromotionAssessment(
            boolean eligible,
            String reasonCode,
            String reasonMessage,
            BigDecimal discountAmount,
            boolean freeShipping
    ) {

        /**
         * Creates an ineligible assessment with zero discount.
         *
         * @param reasonCode reason code
         * @param reasonMessage reason message
         * @return ineligible assessment
         */
        private static PromotionAssessment ineligible(String reasonCode, String reasonMessage) {
            return new PromotionAssessment(false, reasonCode, reasonMessage, ZERO, false);
        }
    }
}
