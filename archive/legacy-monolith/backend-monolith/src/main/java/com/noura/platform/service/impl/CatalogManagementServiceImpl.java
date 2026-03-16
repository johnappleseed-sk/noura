package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.*;
import com.noura.platform.domain.enums.CategoryChangeAction;
import com.noura.platform.domain.enums.CategoryChangeRequestStatus;
import com.noura.platform.domain.enums.OrderStatus;
import com.noura.platform.dto.catalog.*;
import com.noura.platform.repository.*;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.CatalogManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogManagementServiceImpl implements CatalogManagementService {

    private static final Pattern CATEGORY_NAME_PATTERN = Pattern.compile("^[A-Z][A-Za-z0-9&'\\-/ ]{1,79}$");
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[A-Za-z]{2,8}([_-][A-Za-z0-9]{2,8})?$");
    private static final int MAX_CATEGORY_DEPTH = 10;

    private final CategoryRepository categoryRepository;
    private final AttributeRepository attributeRepository;
    private final AttributeSetRepository attributeSetRepository;
    private final UserAccountRepository userAccountRepository;
    private final CategoryTranslationRepository categoryTranslationRepository;
    private final ChannelCategoryMappingRepository channelCategoryMappingRepository;
    private final CategoryChangeRequestRepository categoryChangeRequestRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductInventoryRepository productInventoryRepository;
    private final ProductRepository productRepository;

    /**
     * Creates category.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryDto createCategory(CategoryRequest request) {
        String normalizedName = normalizeCategoryName(request.name());
        categoryRepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
            throw new BadRequestException("CATEGORY_EXISTS", "Category already exists");
        });
        Category category = new Category();
        category.setName(normalizedName);
        category.setDescription(trimToNull(request.description()));
        category.setClassificationCode(normalizeClassificationCode(request.classificationCode()));
        if (request.parentId() != null) {
            Category parent = loadCategory(request.parentId(), "CATEGORY_PARENT_NOT_FOUND", "Parent category not found");
            validateDepth(parent);
            category.setParent(parent);
        }
        if (request.managerId() != null) {
            category.setManager(loadManager(request.managerId()));
        }
        Category saved = categoryRepository.save(category);
        return toCategoryDto(saved);
    }

    /**
     * Updates category.
     *
     * @param categoryId The category id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryDto updateCategory(UUID categoryId, CategoryUpdateRequest request) {
        Category category = loadCategory(categoryId, "CATEGORY_NOT_FOUND", "Category not found");
        String normalizedName = normalizeCategoryName(request.name());
        categoryRepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
            if (!existing.getId().equals(categoryId)) {
                throw new BadRequestException("CATEGORY_EXISTS", "Category already exists");
            }
        });
        category.setName(normalizedName);
        category.setDescription(trimToNull(request.description()));
        category.setClassificationCode(normalizeClassificationCode(request.classificationCode()));

        Category parent = null;
        if (request.parentId() != null) {
            parent = loadCategory(request.parentId(), "CATEGORY_PARENT_NOT_FOUND", "Parent category not found");
            validateNoCycle(category, parent);
            validateDepth(parent);
        }
        category.setParent(parent);

        if (request.managerId() == null) {
            category.setManager(null);
        } else {
            category.setManager(loadManager(request.managerId()));
        }
        category.setTaxonomyVersion(Math.max(1, category.getTaxonomyVersion() + 1));
        return toCategoryDto(categoryRepository.save(category));
    }

    /**
     * Retrieves category tree.
     *
     * @return A list of matching items.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeDto> categoryTree() {
        return categoryTree(null);
    }

    /**
     * Retrieves category tree.
     *
     * @param locale The locale value.
     * @return A list of matching items.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeDto> categoryTree(String locale) {
        Map<UUID, CategoryTranslation> translations = resolveTranslationsByLocale(locale);
        return categoryRepository.findByParentIsNullOrderByNameAsc().stream()
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(category -> toTreeDto(category, translations))
                .toList();
    }

    /**
     * Creates attribute.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AttributeDto createAttribute(AttributeRequest request) {
        attributeRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            throw new BadRequestException("ATTRIBUTE_EXISTS", "Attribute already exists");
        });
        Attribute attribute = new Attribute();
        attribute.setName(request.name().trim());
        attribute.setType(request.type());
        attribute.setPossibleValues(request.possibleValues() == null ? List.of() : request.possibleValues());
        Attribute saved = attributeRepository.save(attribute);
        return new AttributeDto(saved.getId(), saved.getName(), saved.getType(), saved.getPossibleValues());
    }

    /**
     * Creates attribute set.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AttributeSetDto createAttributeSet(AttributeSetRequest request) {
        attributeSetRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            throw new BadRequestException("ATTRIBUTE_SET_EXISTS", "Attribute set already exists");
        });
        Set<Attribute> attributes = request.attributeIds().stream()
                .map(this::loadAttribute)
                .collect(Collectors.toSet());
        AttributeSet attributeSet = new AttributeSet();
        attributeSet.setName(request.name().trim());
        attributeSet.setAttributes(attributes);
        AttributeSet saved = attributeSetRepository.save(attributeSet);
        return new AttributeSetDto(
                saved.getId(),
                saved.getName(),
                saved.getAttributes().stream()
                        .map(item -> new AttributeDto(item.getId(), item.getName(), item.getType(), item.getPossibleValues()))
                        .toList()
        );
    }

    /**
     * Upserts category translation.
     *
     * @param categoryId The category id used to locate the target record.
     * @param locale The locale value.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','B2B')")
    public CategoryTranslationDto upsertCategoryTranslation(UUID categoryId, String locale, CategoryTranslationRequest request) {
        Category category = loadCategory(categoryId, "CATEGORY_NOT_FOUND", "Category not found");
        String normalizedLocale = normalizeLocale(locale);
        CategoryTranslation translation = categoryTranslationRepository
                .findByCategoryIdAndLocaleIgnoreCase(categoryId, normalizedLocale)
                .orElseGet(CategoryTranslation::new);
        translation.setCategory(category);
        translation.setLocale(normalizedLocale);
        translation.setLocalizedName(request.localizedName().trim());
        translation.setLocalizedDescription(trimToNull(request.localizedDescription()));
        translation.setSeoSlug(trimToNull(request.seoSlug()));
        return toTranslationDto(categoryTranslationRepository.save(translation));
    }

    /**
     * Retrieves category translations.
     *
     * @param categoryId The category id used to locate the target record.
     * @return A list of matching items.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryTranslationDto> categoryTranslations(UUID categoryId) {
        loadCategory(categoryId, "CATEGORY_NOT_FOUND", "Category not found");
        return categoryTranslationRepository.findByCategoryIdOrderByLocaleAsc(categoryId)
                .stream()
                .map(this::toTranslationDto)
                .toList();
    }

    /**
     * Creates channel category mapping.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','B2B')")
    public ChannelCategoryMappingDto createChannelCategoryMapping(ChannelCategoryMappingRequest request) {
        Category category = loadCategory(request.categoryId(), "CATEGORY_NOT_FOUND", "Category not found");
        String channel = request.channel().trim().toUpperCase(Locale.ROOT);
        String regionCode = normalizeRegionCode(request.regionCode());
        String externalCategoryId = request.externalCategoryId().trim();
        ChannelCategoryMapping mapping = channelCategoryMappingRepository
                .findByCategoryIdAndChannelIgnoreCaseAndRegionCodeIgnoreCase(category.getId(), channel, regionCode)
                .orElseGet(ChannelCategoryMapping::new);
        mapping.setCategory(category);
        mapping.setChannel(channel);
        mapping.setRegionCode(regionCode);
        mapping.setExternalCategoryId(externalCategoryId);
        mapping.setExternalCategoryName(trimToNull(request.externalCategoryName()));
        mapping.setActive(request.active() == null || request.active());
        return toChannelMappingDto(channelCategoryMappingRepository.save(mapping));
    }

    /**
     * Retrieves category channel mappings.
     *
     * @param categoryId The category id used to locate the target record.
     * @return A list of matching items.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChannelCategoryMappingDto> categoryChannelMappings(UUID categoryId) {
        loadCategory(categoryId, "CATEGORY_NOT_FOUND", "Category not found");
        return channelCategoryMappingRepository.findByCategoryIdOrderByChannelAscRegionCodeAsc(categoryId)
                .stream()
                .map(this::toChannelMappingDto)
                .toList();
    }

    /**
     * Submits category change request.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','B2B')")
    public CategoryChangeRequestDto submitCategoryChangeRequest(CategoryChangeSubmitRequest request) {
        if (request.action() != CategoryChangeAction.CREATE && request.categoryId() == null) {
            throw new BadRequestException("CATEGORY_REQUIRED", "categoryId is required for non-create actions");
        }
        UserAccount requester = loadCurrentUser();
        CategoryChangeRequest changeRequest = new CategoryChangeRequest();
        if (request.categoryId() != null) {
            changeRequest.setCategory(loadCategory(request.categoryId(), "CATEGORY_NOT_FOUND", "Category not found"));
        }
        changeRequest.setAction(request.action());
        changeRequest.setStatus(CategoryChangeRequestStatus.PENDING);
        changeRequest.setRequestedBy(requester);
        changeRequest.setPayload(request.payload() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.payload()));
        changeRequest.setReason(trimToNull(request.reason()));
        return toChangeRequestDto(categoryChangeRequestRepository.save(changeRequest));
    }

    /**
     * Retrieves category change requests.
     *
     * @param status The status value.
     * @return A list of matching items.
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','B2B')")
    public List<CategoryChangeRequestDto> categoryChangeRequests(CategoryChangeRequestStatus status) {
        List<CategoryChangeRequest> requests = status == null
                ? categoryChangeRequestRepository.findAllByOrderByCreatedAtDesc()
                : categoryChangeRequestRepository.findByStatusOrderByCreatedAtDesc(status);
        return requests.stream().map(this::toChangeRequestDto).toList();
    }

    /**
     * Approves category change request.
     *
     * @param requestId The request id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryChangeRequestDto approveCategoryChangeRequest(UUID requestId, CategoryChangeReviewRequest request) {
        CategoryChangeRequest changeRequest = loadChangeRequest(requestId);
        if (changeRequest.getStatus() != CategoryChangeRequestStatus.PENDING) {
            throw new BadRequestException("CATEGORY_CHANGE_FINALIZED", "Category change request is already finalized");
        }
        applyApprovedCategoryChange(changeRequest);
        changeRequest.setStatus(CategoryChangeRequestStatus.APPROVED);
        changeRequest.setReviewComment(trimToNull(request.reviewComment()));
        changeRequest.setReviewedBy(loadCurrentUser());
        changeRequest.setReviewedAt(Instant.now());
        return toChangeRequestDto(categoryChangeRequestRepository.save(changeRequest));
    }

    /**
     * Rejects category change request.
     *
     * @param requestId The request id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryChangeRequestDto rejectCategoryChangeRequest(UUID requestId, CategoryChangeReviewRequest request) {
        CategoryChangeRequest changeRequest = loadChangeRequest(requestId);
        if (changeRequest.getStatus() != CategoryChangeRequestStatus.PENDING) {
            throw new BadRequestException("CATEGORY_CHANGE_FINALIZED", "Category change request is already finalized");
        }
        changeRequest.setStatus(CategoryChangeRequestStatus.REJECTED);
        changeRequest.setReviewComment(trimToNull(request.reviewComment()));
        changeRequest.setReviewedBy(loadCurrentUser());
        changeRequest.setReviewedAt(Instant.now());
        return toChangeRequestDto(categoryChangeRequestRepository.save(changeRequest));
    }

    /**
     * Suggests category.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional(readOnly = true)
    public CategorySuggestionResponse suggestCategory(CategorySuggestionRequest request) {
        String normalizedText = normalizeInputText(request);
        Set<String> inputTokens = tokenize(normalizedText);
        int maxSuggestions = request.maxSuggestions() == null ? 3 : Math.max(1, Math.min(10, request.maxSuggestions()));
        Map<UUID, CategoryTranslation> localeTranslations = resolveTranslationsByLocale(request.locale());

        List<CategorySuggestionItemDto> scored = categoryRepository.findAll().stream()
                .map(category -> scoreCategorySuggestion(category, normalizedText, inputTokens, localeTranslations))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CategorySuggestionItemDto::confidence).reversed())
                .limit(maxSuggestions)
                .toList();

        if (!scored.isEmpty()) {
            return new CategorySuggestionResponse(scored);
        }
        List<CategorySuggestionItemDto> fallback = categoryRepository.findByParentIsNullOrderByNameAsc().stream()
                .limit(maxSuggestions)
                .map(category -> new CategorySuggestionItemDto(
                        category.getId(),
                        category.getName(),
                        0.10,
                        List.of("fallback_top_level")
                ))
                .toList();
        return new CategorySuggestionResponse(fallback);
    }

    /**
     * Retrieves category analytics.
     *
     * @param from The from value.
     * @param to The to value.
     * @return A list of matching items.
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','B2B')")
    public List<CategoryAnalyticsDto> categoryAnalytics(Instant from, Instant to) {
        Instant windowEnd = to == null ? Instant.now() : to;
        Instant windowStart = from == null ? windowEnd.minus(30, ChronoUnit.DAYS) : from;
        if (windowEnd.isBefore(windowStart)) {
            throw new BadRequestException("ANALYTICS_WINDOW_INVALID", "'to' must be after 'from'");
        }

        List<OrderStatus> statuses = List.of(
                OrderStatus.PAID,
                OrderStatus.PACKED,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED,
                OrderStatus.REFUNDED
        );
        List<Order> orders = orderRepository.findByCreatedAtBetweenAndStatusIn(windowStart, windowEnd, statuses);
        List<OrderItem> items = orders.isEmpty() ? List.of() : orderItemRepository.findByOrderIn(orders);

        Map<UUID, BigDecimal> revenueByCategory = new HashMap<>();
        Map<UUID, Long> unitsByCategory = new HashMap<>();
        Map<UUID, Set<UUID>> orderIdsByCategory = new HashMap<>();
        for (OrderItem item : items) {
            if (item.getProduct() == null || item.getProduct().getCategory() == null) {
                continue;
            }
            UUID categoryId = item.getProduct().getCategory().getId();
            revenueByCategory.merge(categoryId, item.getLineTotal(), BigDecimal::add);
            unitsByCategory.merge(categoryId, (long) item.getQuantity(), Long::sum);
            orderIdsByCategory.computeIfAbsent(categoryId, ignored -> new HashSet<>()).add(item.getOrder().getId());
        }

        Map<UUID, Long> stockByCategory = productInventoryRepository.findAll().stream()
                .filter(inventory -> inventory.getProduct() != null && inventory.getProduct().getCategory() != null)
                .collect(Collectors.groupingBy(
                        inventory -> inventory.getProduct().getCategory().getId(),
                        Collectors.summingLong(ProductInventory::getStock)
                ));

        Map<UUID, Double> discoverabilityByCategory = productRepository.findAll().stream()
                .filter(Product::isActive)
                .filter(product -> product.getCategory() != null)
                .collect(Collectors.groupingBy(
                        product -> product.getCategory().getId(),
                        Collectors.averagingInt(Product::getPopularityScore)
                ));

        return categoryRepository.findAll().stream()
                .map(category -> {
                    UUID categoryId = category.getId();
                    BigDecimal revenue = revenueByCategory.getOrDefault(categoryId, BigDecimal.ZERO);
                    long unitsSold = unitsByCategory.getOrDefault(categoryId, 0L);
                    long orderCount = orderIdsByCategory.getOrDefault(categoryId, Set.of()).size();
                    long currentStock = stockByCategory.getOrDefault(categoryId, 0L);
                    BigDecimal turnover = currentStock <= 0
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(unitsSold).divide(BigDecimal.valueOf(currentStock), 4, RoundingMode.HALF_UP);
                    Double discoverability = discoverabilityByCategory.get(categoryId);
                    return new CategoryAnalyticsDto(
                            categoryId,
                            category.getName(),
                            revenue,
                            unitsSold,
                            orderCount,
                            currentStock,
                            turnover,
                            discoverability,
                            null,
                            null
                    );
                })
                .sorted(Comparator.comparing(CategoryAnalyticsDto::revenue).reversed())
                .toList();
    }

    private void applyApprovedCategoryChange(CategoryChangeRequest changeRequest) {
        CategoryChangeAction action = changeRequest.getAction();
        Map<String, Object> payload = changeRequest.getPayload() == null
                ? Map.of()
                : changeRequest.getPayload();
        switch (action) {
            case CREATE -> applyCreateCategoryChange(changeRequest, payload);
            case UPDATE -> applyUpdateCategoryChange(requireChangeCategory(changeRequest), payload);
            case RENAME -> applyRenameCategoryChange(requireChangeCategory(changeRequest), payload);
            case MOVE -> applyMoveCategoryChange(requireChangeCategory(changeRequest), payload);
            case DELETE -> applyDeleteCategoryChange(changeRequest, requireChangeCategory(changeRequest));
            default -> throw new BadRequestException("CATEGORY_CHANGE_ACTION_UNSUPPORTED", "Unsupported category change action");
        }
    }

    private void applyCreateCategoryChange(CategoryChangeRequest changeRequest, Map<String, Object> payload) {
        String normalizedName = normalizeCategoryName(readRequiredPayloadString(payload, "name"));
        ensureUniqueCategoryName(normalizedName, null);

        Category category = new Category();
        category.setName(normalizedName);
        category.setDescription(trimToNull(readOptionalPayloadString(payload, "description")));
        category.setClassificationCode(normalizeClassificationCode(readOptionalPayloadString(payload, "classificationCode")));

        if (payload.containsKey("parentId")) {
            UUID parentId = readOptionalPayloadUuid(payload, "parentId");
            if (parentId != null) {
                Category parent = loadCategory(parentId, "CATEGORY_PARENT_NOT_FOUND", "Parent category not found");
                validateDepth(parent);
                category.setParent(parent);
            }
        }
        if (payload.containsKey("managerId")) {
            UUID managerId = readOptionalPayloadUuid(payload, "managerId");
            if (managerId != null) {
                category.setManager(loadManager(managerId));
            }
        }
        category.setTaxonomyVersion(1);
        changeRequest.setCategory(categoryRepository.save(category));
    }

    private void applyUpdateCategoryChange(Category category, Map<String, Object> payload) {
        boolean changed = false;
        if (payload.containsKey("name")) {
            String normalizedName = normalizeCategoryName(readRequiredPayloadString(payload, "name"));
            ensureUniqueCategoryName(normalizedName, category.getId());
            category.setName(normalizedName);
            changed = true;
        }
        if (payload.containsKey("description")) {
            category.setDescription(trimToNull(readOptionalPayloadString(payload, "description")));
            changed = true;
        }
        if (payload.containsKey("classificationCode")) {
            category.setClassificationCode(normalizeClassificationCode(readOptionalPayloadString(payload, "classificationCode")));
            changed = true;
        }
        if (payload.containsKey("parentId")) {
            UUID parentId = readOptionalPayloadUuid(payload, "parentId");
            Category parent = null;
            if (parentId != null) {
                parent = loadCategory(parentId, "CATEGORY_PARENT_NOT_FOUND", "Parent category not found");
                validateNoCycle(category, parent);
                validateDepth(parent);
            }
            category.setParent(parent);
            changed = true;
        }
        if (payload.containsKey("managerId")) {
            UUID managerId = readOptionalPayloadUuid(payload, "managerId");
            category.setManager(managerId == null ? null : loadManager(managerId));
            changed = true;
        }
        if (!changed) {
            throw new BadRequestException(
                    "CATEGORY_CHANGE_PAYLOAD_INVALID",
                    "Payload must contain at least one of: name, description, classificationCode, parentId, managerId"
            );
        }
        bumpTaxonomyVersion(category);
    }

    private void applyRenameCategoryChange(Category category, Map<String, Object> payload) {
        String normalizedName = normalizeCategoryName(readRequiredPayloadString(payload, "name"));
        ensureUniqueCategoryName(normalizedName, category.getId());
        category.setName(normalizedName);
        bumpTaxonomyVersion(category);
    }

    private void applyMoveCategoryChange(Category category, Map<String, Object> payload) {
        if (!payload.containsKey("parentId")) {
            throw new BadRequestException("CATEGORY_CHANGE_PAYLOAD_INVALID", "MOVE action requires parentId in payload");
        }
        UUID parentId = readOptionalPayloadUuid(payload, "parentId");
        Category parent = null;
        if (parentId != null) {
            parent = loadCategory(parentId, "CATEGORY_PARENT_NOT_FOUND", "Parent category not found");
            validateNoCycle(category, parent);
            validateDepth(parent);
        }
        category.setParent(parent);
        bumpTaxonomyVersion(category);
    }

    private void applyDeleteCategoryChange(CategoryChangeRequest changeRequest, Category category) {
        if (categoryRepository.existsByParentId(category.getId())) {
            throw new BadRequestException("CATEGORY_HAS_CHILDREN", "Cannot delete category with child categories");
        }
        if (productRepository.existsByCategoryId(category.getId())) {
            throw new BadRequestException("CATEGORY_HAS_PRODUCTS", "Cannot delete category with linked products");
        }
        categoryRepository.delete(category);
        changeRequest.setCategory(null);
    }

    private void bumpTaxonomyVersion(Category category) {
        category.setTaxonomyVersion(Math.max(1, category.getTaxonomyVersion() + 1));
        categoryRepository.save(category);
    }

    private void ensureUniqueCategoryName(String normalizedName, UUID categoryIdToIgnore) {
        categoryRepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
            if (categoryIdToIgnore == null || !existing.getId().equals(categoryIdToIgnore)) {
                throw new BadRequestException("CATEGORY_EXISTS", "Category already exists");
            }
        });
    }

    private String readRequiredPayloadString(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key) || payload.get(key) == null) {
            throw new BadRequestException("CATEGORY_CHANGE_PAYLOAD_INVALID", key + " is required");
        }
        Object raw = payload.get(key);
        if (!(raw instanceof String value)) {
            throw new BadRequestException("CATEGORY_CHANGE_PAYLOAD_INVALID", key + " must be a string");
        }
        return value;
    }

    private String readOptionalPayloadString(Map<String, Object> payload, String key) {
        Object raw = payload.get(key);
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof String value)) {
            throw new BadRequestException("CATEGORY_CHANGE_PAYLOAD_INVALID", key + " must be a string");
        }
        return value;
    }

    private UUID readOptionalPayloadUuid(Map<String, Object> payload, String key) {
        Object raw = payload.get(key);
        if (raw == null) {
            return null;
        }
        if (raw instanceof UUID uuid) {
            return uuid;
        }
        if (raw instanceof String value) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return UUID.fromString(trimmed);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("CATEGORY_CHANGE_PAYLOAD_INVALID", key + " must be a valid UUID");
            }
        }
        throw new BadRequestException("CATEGORY_CHANGE_PAYLOAD_INVALID", key + " must be a UUID string");
    }

    private Category requireChangeCategory(CategoryChangeRequest changeRequest) {
        if (changeRequest.getCategory() == null) {
            throw new BadRequestException("CATEGORY_REQUIRED", "categoryId is required for this change action");
        }
        return changeRequest.getCategory();
    }

    private CategorySuggestionItemDto scoreCategorySuggestion(
            Category category,
            String normalizedText,
            Set<String> inputTokens,
            Map<UUID, CategoryTranslation> localeTranslations
    ) {
        List<String> signals = new ArrayList<>();
        double score = 0D;

        String candidateName = category.getName();
        CategoryTranslation translation = localeTranslations.get(category.getId());
        if (translation != null && translation.getLocalizedName() != null && !translation.getLocalizedName().isBlank()) {
            candidateName = translation.getLocalizedName();
        }
        String normalizedCategoryName = candidateName.toLowerCase(Locale.ROOT);
        if (normalizedText.contains(normalizedCategoryName)) {
            score += 3D;
            signals.add("name_phrase_match");
        }
        for (String token : tokenize(normalizedCategoryName)) {
            if (token.length() < 3) {
                continue;
            }
            if (inputTokens.contains(token)) {
                score += 1D;
                signals.add("token:" + token);
            }
        }
        if (category.getParent() != null) {
            String parentName = category.getParent().getName().toLowerCase(Locale.ROOT);
            if (normalizedText.contains(parentName)) {
                score += 0.5D;
                signals.add("parent_phrase_match");
            }
        }
        if (score <= 0D) {
            return null;
        }
        double confidence = Math.min(0.99D, score / 10D);
        return new CategorySuggestionItemDto(
                category.getId(),
                candidateName,
                confidence,
                signals.stream().distinct().toList()
        );
    }

    private String normalizeInputText(CategorySuggestionRequest request) {
        StringBuilder source = new StringBuilder();
        source.append(request.title());
        if (request.description() != null) {
            source.append(' ').append(request.description());
        }
        if (request.attributes() != null) {
            request.attributes().forEach((key, value) -> {
                source.append(' ').append(key);
                if (value != null) {
                    source.append(' ').append(value);
                }
            });
        }
        return source.toString().toLowerCase(Locale.ROOT);
    }

    private Set<String> tokenize(String value) {
        return Arrays.stream(value.split("[^a-z0-9]+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private CategoryDto toCategoryDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getClassificationCode(),
                category.getParent() == null ? null : category.getParent().getId(),
                category.getManager() == null ? null : category.getManager().getId(),
                category.getTaxonomyVersion()
        );
    }

    private CategoryTreeDto toTreeDto(Category category, Map<UUID, CategoryTranslation> translations) {
        String name = category.getName();
        String description = category.getDescription();
        CategoryTranslation translation = translations.get(category.getId());
        if (translation != null) {
            if (translation.getLocalizedName() != null && !translation.getLocalizedName().isBlank()) {
                name = translation.getLocalizedName();
            }
            if (translation.getLocalizedDescription() != null && !translation.getLocalizedDescription().isBlank()) {
                description = translation.getLocalizedDescription();
            }
        }
        return new CategoryTreeDto(
                category.getId(),
                name,
                description,
                category.getClassificationCode(),
                category.getManager() == null ? null : category.getManager().getId(),
                category.getChildren().stream()
                        .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                        .map(child -> toTreeDto(child, translations))
                        .toList()
        );
    }

    private CategoryTranslationDto toTranslationDto(CategoryTranslation translation) {
        return new CategoryTranslationDto(
                translation.getId(),
                translation.getCategory().getId(),
                translation.getLocale(),
                translation.getLocalizedName(),
                translation.getLocalizedDescription(),
                translation.getSeoSlug()
        );
    }

    private ChannelCategoryMappingDto toChannelMappingDto(ChannelCategoryMapping mapping) {
        return new ChannelCategoryMappingDto(
                mapping.getId(),
                mapping.getCategory().getId(),
                mapping.getChannel(),
                mapping.getRegionCode(),
                mapping.getExternalCategoryId(),
                mapping.getExternalCategoryName(),
                mapping.isActive()
        );
    }

    private CategoryChangeRequestDto toChangeRequestDto(CategoryChangeRequest request) {
        return new CategoryChangeRequestDto(
                request.getId(),
                request.getCategory() == null ? null : request.getCategory().getId(),
                request.getAction(),
                request.getStatus(),
                request.getRequestedBy().getId(),
                request.getReviewedBy() == null ? null : request.getReviewedBy().getId(),
                request.getPayload(),
                request.getReason(),
                request.getReviewComment(),
                request.getCreatedAt(),
                request.getReviewedAt()
        );
    }

    private CategoryChangeRequest loadChangeRequest(UUID requestId) {
        return categoryChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("CATEGORY_CHANGE_NOT_FOUND", "Category change request not found"));
    }

    private Category loadCategory(UUID categoryId, String code, String message) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(code, message));
    }

    private Attribute loadAttribute(UUID attributeId) {
        return attributeRepository.findById(attributeId)
                .orElseThrow(() -> new NotFoundException("ATTRIBUTE_NOT_FOUND", "Attribute not found"));
    }

    private UserAccount loadManager(UUID managerId) {
        return userAccountRepository.findById(managerId)
                .orElseThrow(() -> new NotFoundException("CATEGORY_MANAGER_NOT_FOUND", "Category manager not found"));
    }

    private UserAccount loadCurrentUser() {
        return userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private void validateDepth(Category parent) {
        int depth = 1;
        Category cursor = parent;
        while (cursor != null) {
            depth++;
            if (depth > MAX_CATEGORY_DEPTH) {
                throw new BadRequestException("CATEGORY_DEPTH_EXCEEDED", "Category hierarchy depth limit exceeded");
            }
            cursor = cursor.getParent();
        }
    }

    private void validateNoCycle(Category category, Category parent) {
        Category cursor = parent;
        while (cursor != null) {
            if (cursor.getId().equals(category.getId())) {
                throw new BadRequestException("CATEGORY_CYCLE", "Category cannot be its own ancestor");
            }
            cursor = cursor.getParent();
        }
    }

    private String normalizeCategoryName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isBlank()) {
            throw new BadRequestException("CATEGORY_NAME_REQUIRED", "Category name is required");
        }
        if (name.contains("  ")) {
            throw new BadRequestException("CATEGORY_NAME_INVALID", "Category name cannot contain repeated spaces");
        }
        if (!CATEGORY_NAME_PATTERN.matcher(name).matches()) {
            throw new BadRequestException(
                    "CATEGORY_NAME_INVALID",
                    "Category name must start with uppercase and contain letters, numbers, spaces, &, ', -, /"
            );
        }
        return name;
    }

    private String normalizeClassificationCode(String code) {
        String normalized = trimToNull(code);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > 120) {
            throw new BadRequestException("CATEGORY_CODE_INVALID", "classificationCode must be at most 120 characters");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeLocale(String locale) {
        String normalized = locale == null ? "" : locale.trim();
        if (normalized.isBlank()) {
            throw new BadRequestException("LOCALE_REQUIRED", "Locale is required");
        }
        if (!LOCALE_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException("LOCALE_INVALID", "Locale format is invalid");
        }
        String[] parts = normalized.replace('_', '-').split("-");
        if (parts.length == 1) {
            return parts[0].toLowerCase(Locale.ROOT);
        }
        return parts[0].toLowerCase(Locale.ROOT) + "-" + parts[1].toUpperCase(Locale.ROOT);
    }

    private String normalizeRegionCode(String regionCode) {
        String normalized = trimToNull(regionCode);
        if (normalized == null) {
            return "GLOBAL";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<UUID, CategoryTranslation> resolveTranslationsByLocale(String locale) {
        String normalized = trimToNull(locale);
        if (normalized == null) {
            return Map.of();
        }
        List<CategoryTranslation> translations = categoryTranslationRepository.findByLocaleIgnoreCase(normalized);
        return translations.stream().collect(Collectors.toMap(
                translation -> translation.getCategory().getId(),
                translation -> translation,
                (left, right) -> left
        ));
    }
}
