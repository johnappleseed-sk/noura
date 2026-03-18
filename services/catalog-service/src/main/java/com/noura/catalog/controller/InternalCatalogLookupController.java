package com.noura.catalog.controller;

import com.noura.catalog.common.ApiResponse;
import com.noura.catalog.dto.product.VariantLookupResponse;
import com.noura.catalog.service.CatalogQueryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Internal compatibility lookups used by other extracted services.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/catalog")
public class InternalCatalogLookupController {

    private final CatalogQueryService catalogQueryService;

    /**
     * Resolves one variant to its owning product.
     *
     * @param variantId variant identifier
     * @param request current request
     * @return variant lookup response
     */
    @GetMapping("/variants/{variantId}")
    public ApiResponse<VariantLookupResponse> getVariant(
            @PathVariable UUID variantId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Variant lookup",
                catalogQueryService.lookupVariant(variantId),
                request.getRequestURI()
        );
    }
}
