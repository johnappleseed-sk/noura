package com.noura.platform.service;

import com.noura.platform.dto.catalog.*;
import com.noura.platform.domain.enums.CategoryChangeRequestStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CatalogManagementService {
    /**
     * Creates category.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    CategoryDto createCategory(CategoryRequest request);

    /**
     * Updates category.
     *
     * @param categoryId The category id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    CategoryDto updateCategory(UUID categoryId, CategoryUpdateRequest request);

    /**
     * Retrieves category tree.
     *
     * @return A list of matching items.
     */
    List<CategoryTreeDto> categoryTree();

    /**
     * Retrieves category tree.
     *
     * @param locale The locale value.
     * @return A list of matching items.
     */
    List<CategoryTreeDto> categoryTree(String locale);

    /**
     * Creates attribute.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    AttributeDto createAttribute(AttributeRequest request);

    /**
     * Creates attribute set.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    AttributeSetDto createAttributeSet(AttributeSetRequest request);

    /**
     * Upserts category translation.
     *
     * @param categoryId The category id used to locate the target record.
     * @param locale The locale value.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    CategoryTranslationDto upsertCategoryTranslation(UUID categoryId, String locale, CategoryTranslationRequest request);

    /**
     * Retrieves category translations.
     *
     * @param categoryId The category id used to locate the target record.
     * @return A list of matching items.
     */
    List<CategoryTranslationDto> categoryTranslations(UUID categoryId);

    /**
     * Creates channel category mapping.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    ChannelCategoryMappingDto createChannelCategoryMapping(ChannelCategoryMappingRequest request);

    /**
     * Retrieves category channel mappings.
     *
     * @param categoryId The category id used to locate the target record.
     * @return A list of matching items.
     */
    List<ChannelCategoryMappingDto> categoryChannelMappings(UUID categoryId);

    /**
     * Submits category change request.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    CategoryChangeRequestDto submitCategoryChangeRequest(CategoryChangeSubmitRequest request);

    /**
     * Retrieves category change requests.
     *
     * @param status The status value.
     * @return A list of matching items.
     */
    List<CategoryChangeRequestDto> categoryChangeRequests(CategoryChangeRequestStatus status);

    /**
     * Approves category change request.
     *
     * @param requestId The request id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    CategoryChangeRequestDto approveCategoryChangeRequest(UUID requestId, CategoryChangeReviewRequest request);

    /**
     * Rejects category change request.
     *
     * @param requestId The request id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    CategoryChangeRequestDto rejectCategoryChangeRequest(UUID requestId, CategoryChangeReviewRequest request);

    /**
     * Suggests category.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    CategorySuggestionResponse suggestCategory(CategorySuggestionRequest request);

    /**
     * Retrieves category analytics.
     *
     * @param from The from value.
     * @param to The to value.
     * @return A list of matching items.
     */
    List<CategoryAnalyticsDto> categoryAnalytics(Instant from, Instant to);
}
