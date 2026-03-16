package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.domain.enums.ProductSubmissionStatus;
import com.noura.platform.dto.submission.ProductSubmissionCreateRequest;
import com.noura.platform.dto.submission.ProductSubmissionDetailDto;
import com.noura.platform.dto.submission.ProductSubmissionDto;
import com.noura.platform.service.ProductSubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/stores/{storeId}/product-submissions")
public class StoreProductSubmissionController {

    private final ProductSubmissionService productSubmissionService;

    @GetMapping
    public ApiResponse<PageResponse<ProductSubmissionDto>> list(
            @PathVariable UUID storeId,
            @RequestParam(required = false) ProductSubmissionStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<ProductSubmissionDto> submissions = productSubmissionService.listForStore(storeId, status, pageable);
        return ApiResponse.ok("Product submissions", PageResponse.from(submissions), http.getRequestURI());
    }

    @GetMapping("/{submissionId}")
    public ApiResponse<ProductSubmissionDetailDto> get(
            @PathVariable UUID storeId,
            @PathVariable UUID submissionId,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Product submission", productSubmissionService.getForStore(storeId, submissionId), http.getRequestURI());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductSubmissionDto>> submit(
            @PathVariable UUID storeId,
            @Valid @RequestBody ProductSubmissionCreateRequest request,
            HttpServletRequest http
    ) {
        ProductSubmissionDto created = productSubmissionService.submit(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product submission created", created, http.getRequestURI()));
    }

    @PostMapping("/{submissionId}/resubmit")
    public ResponseEntity<ApiResponse<ProductSubmissionDto>> resubmit(
            @PathVariable UUID storeId,
            @PathVariable UUID submissionId,
            @Valid @RequestBody ProductSubmissionCreateRequest request,
            HttpServletRequest http
    ) {
        ProductSubmissionDto created = productSubmissionService.resubmit(storeId, submissionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product submission resubmitted", created, http.getRequestURI()));
    }
}

