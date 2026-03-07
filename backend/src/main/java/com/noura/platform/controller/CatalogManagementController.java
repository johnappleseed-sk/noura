package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.domain.enums.CategoryChangeRequestStatus;
import com.noura.platform.dto.catalog.*;
import com.noura.platform.service.CatalogManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CatalogManagementController {

    private final CatalogManagementService catalogManagementService;

    /**
     * Creates category.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("${app.api.version-prefix:/api/v1}/categories")
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(
            @Valid @RequestBody CategoryRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Category created", catalogManagementService.createCategory(request), http.getRequestURI()));
    }

    /**
     * Retrieves category tree.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("${app.api.version-prefix:/api/v1}/categories/tree")
    public ApiResponse<List<CategoryTreeDto>> categoryTree(
            @RequestParam(required = false) String locale,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Category tree", catalogManagementService.categoryTree(locale), http.getRequestURI());
    }

    /**
     * Updates category.
     *
     * @param categoryId The category id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PutMapping("${app.api.version-prefix:/api/v1}/categories/{categoryId}")
    public ApiResponse<CategoryDto> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryUpdateRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Category updated", catalogManagementService.updateCategory(categoryId, request), http.getRequestURI());
    }

    /**
     * Creates attribute.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("${app.api.version-prefix:/api/v1}/attributes")
    public ResponseEntity<ApiResponse<AttributeDto>> createAttribute(
            @Valid @RequestBody AttributeRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Attribute created", catalogManagementService.createAttribute(request), http.getRequestURI()));
    }

    /**
     * Creates attribute set.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("${app.api.version-prefix:/api/v1}/attribute-sets")
    public ResponseEntity<ApiResponse<AttributeSetDto>> createAttributeSet(
            @Valid @RequestBody AttributeSetRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Attribute set created", catalogManagementService.createAttributeSet(request), http.getRequestURI()));
    }

    /**
     * Upserts category translation.
     *
     * @param categoryId The category id used to locate the target record.
     * @param locale The locale value.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PutMapping("${app.api.version-prefix:/api/v1}/categories/{categoryId}/translations/{locale}")
    public ApiResponse<CategoryTranslationDto> upsertCategoryTranslation(
            @PathVariable UUID categoryId,
            @PathVariable String locale,
            @Valid @RequestBody CategoryTranslationRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Category translation saved",
                catalogManagementService.upsertCategoryTranslation(categoryId, locale, request),
                http.getRequestURI()
        );
    }

    /**
     * Retrieves category translations.
     *
     * @param categoryId The category id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("${app.api.version-prefix:/api/v1}/categories/{categoryId}/translations")
    public ApiResponse<List<CategoryTranslationDto>> categoryTranslations(
            @PathVariable UUID categoryId,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Category translations",
                catalogManagementService.categoryTranslations(categoryId),
                http.getRequestURI()
        );
    }

    /**
     * Creates channel category mapping.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("${app.api.version-prefix:/api/v1}/categories/channel-mappings")
    public ResponseEntity<ApiResponse<ChannelCategoryMappingDto>> createChannelCategoryMapping(
            @Valid @RequestBody ChannelCategoryMappingRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Channel category mapping saved",
                        catalogManagementService.createChannelCategoryMapping(request),
                        http.getRequestURI()
                ));
    }

    /**
     * Retrieves category channel mappings.
     *
     * @param categoryId The category id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("${app.api.version-prefix:/api/v1}/categories/{categoryId}/channel-mappings")
    public ApiResponse<List<ChannelCategoryMappingDto>> categoryChannelMappings(
            @PathVariable UUID categoryId,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Category channel mappings",
                catalogManagementService.categoryChannelMappings(categoryId),
                http.getRequestURI()
        );
    }

    /**
     * Submits category change request.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("${app.api.version-prefix:/api/v1}/categories/change-requests")
    public ResponseEntity<ApiResponse<CategoryChangeRequestDto>> submitCategoryChangeRequest(
            @Valid @RequestBody CategoryChangeSubmitRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Category change request submitted",
                        catalogManagementService.submitCategoryChangeRequest(request),
                        http.getRequestURI()
                ));
    }

    /**
     * Retrieves category change requests.
     *
     * @param status The status value.
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("${app.api.version-prefix:/api/v1}/categories/change-requests")
    public ApiResponse<List<CategoryChangeRequestDto>> categoryChangeRequests(
            @RequestParam(required = false) CategoryChangeRequestStatus status,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Category change requests",
                catalogManagementService.categoryChangeRequests(status),
                http.getRequestURI()
        );
    }

    /**
     * Approves category change request.
     *
     * @param requestId The request id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PatchMapping("${app.api.version-prefix:/api/v1}/categories/change-requests/{requestId}/approve")
    public ApiResponse<CategoryChangeRequestDto> approveCategoryChangeRequest(
            @PathVariable UUID requestId,
            @RequestBody CategoryChangeReviewRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Category change request approved",
                catalogManagementService.approveCategoryChangeRequest(requestId, request),
                http.getRequestURI()
        );
    }

    /**
     * Rejects category change request.
     *
     * @param requestId The request id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PatchMapping("${app.api.version-prefix:/api/v1}/categories/change-requests/{requestId}/reject")
    public ApiResponse<CategoryChangeRequestDto> rejectCategoryChangeRequest(
            @PathVariable UUID requestId,
            @RequestBody CategoryChangeReviewRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Category change request rejected",
                catalogManagementService.rejectCategoryChangeRequest(requestId, request),
                http.getRequestURI()
        );
    }

    /**
     * Suggests category.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("${app.api.version-prefix:/api/v1}/categories/ai/suggest")
    public ApiResponse<CategorySuggestionResponse> suggestCategory(
            @Valid @RequestBody CategorySuggestionRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Category suggestions", catalogManagementService.suggestCategory(request), http.getRequestURI());
    }

    /**
     * Retrieves category analytics.
     *
     * @param from The from value.
     * @param to The to value.
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("${app.api.version-prefix:/api/v1}/categories/analytics")
    public ApiResponse<List<CategoryAnalyticsDto>> categoryAnalytics(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Category analytics",
                catalogManagementService.categoryAnalytics(from, to),
                http.getRequestURI()
        );
    }
}
