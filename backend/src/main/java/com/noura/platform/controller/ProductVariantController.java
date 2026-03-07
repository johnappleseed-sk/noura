package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.product.ProductVariantDto;
import com.noura.platform.dto.product.ProductVariantRequest;
import com.noura.platform.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/variants")
public class ProductVariantController {

    private final ProductService productService;

    /**
     * Updates resource.
     *
     * @param variantId The variant id used to locate the target record.
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PutMapping("/{variantId}")
    public ApiResponse<ProductVariantDto> update(
            @PathVariable UUID variantId,
            @Valid @RequestBody ProductVariantRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Variant updated", productService.updateVariant(variantId, request), http.getRequestURI());
    }
}
