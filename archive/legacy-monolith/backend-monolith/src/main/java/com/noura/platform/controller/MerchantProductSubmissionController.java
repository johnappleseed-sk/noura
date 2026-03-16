package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.superinventory.CreateProductSubmissionRequest;
import com.noura.platform.dto.superinventory.ProductSubmissionResponse;
import com.noura.platform.service.SuperInventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/merchant/product-submissions", "${app.api.version-prefix:/api/v1}/merchant/product-submissions"})
public class MerchantProductSubmissionController {

    private final SuperInventoryService superInventoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductSubmissionResponse>> submit(
            @Valid @RequestBody CreateProductSubmissionRequest request,
            HttpServletRequest http
    ) {
        ProductSubmissionResponse response = superInventoryService.submitProductCandidate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product submission created", response, http.getRequestURI()));
    }
}
