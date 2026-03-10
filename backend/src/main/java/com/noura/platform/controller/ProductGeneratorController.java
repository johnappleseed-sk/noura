package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.product.ProductGeneratorRequest;
import com.noura.platform.dto.product.ProductGeneratorResponse;
import com.noura.platform.service.ProductGeneratorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/admin/product-generator")
public class ProductGeneratorController {

    private final ProductGeneratorService productGeneratorService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ProductGeneratorResponse>> generate(
            @RequestBody(required = false) ProductGeneratorRequest request,
            HttpServletRequest http
    ) {
        if (request == null) {
            request = new ProductGeneratorRequest();
        }
        ProductGeneratorResponse result = productGeneratorService.generate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product generated", result, http.getRequestURI()));
    }
}
