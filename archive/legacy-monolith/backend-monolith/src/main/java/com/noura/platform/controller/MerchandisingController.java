package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.dto.merchandising.MerchandisingProductDto;
import com.noura.platform.service.MerchandisingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MerchandisingController {

    private final MerchandisingService merchandisingService;

    @GetMapping("${app.api.version-prefix:/api/v1}/merchandising/products")
    public ApiResponse<PageResponse<MerchandisingProductDto>> listProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "featured") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Merchandised products",
                merchandisingService.listProducts(query, categoryId, storeId, sort, page, size),
                http.getRequestURI()
        );
    }
}
