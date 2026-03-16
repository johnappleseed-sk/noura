package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.domain.enums.SubmissionStatus;
import com.noura.platform.dto.superinventory.ApproveProductSubmissionRequest;
import com.noura.platform.dto.superinventory.ProductSubmissionDetailResponse;
import com.noura.platform.dto.superinventory.ProductSubmissionResponse;
import com.noura.platform.dto.superinventory.RejectProductSubmissionRequest;
import com.noura.platform.service.SuperInventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/product-submissions")
public class AdminProductSubmissionController {

    private final SuperInventoryService superInventoryService;

    @GetMapping
    public ApiResponse<PageResponse<ProductSubmissionResponse>> list(
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) UUID merchantId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<ProductSubmissionResponse> submissions = superInventoryService
                .listProductSubmissions(status, merchantId, storeId, query, pageable);
        return ApiResponse.ok("Product submissions", PageResponse.from(submissions), http.getRequestURI());
    }

    @GetMapping("/{submissionId}")
    public ApiResponse<ProductSubmissionDetailResponse> get(
            @PathVariable UUID submissionId,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Product submission",
                superInventoryService.getProductSubmission(submissionId),
                http.getRequestURI()
        );
    }

    @PostMapping("/{submissionId}/approve")
    public ApiResponse<ProductSubmissionResponse> approve(
            @PathVariable UUID submissionId,
            @RequestBody(required = false) @Valid ApproveProductSubmissionRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Product submission approved",
                superInventoryService.approveProductSubmission(submissionId, request),
                http.getRequestURI()
        );
    }

    @PostMapping("/{submissionId}/reject")
    public ApiResponse<ProductSubmissionResponse> reject(
            @PathVariable UUID submissionId,
            @RequestBody(required = false) @Valid RejectProductSubmissionRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Product submission rejected",
                superInventoryService.rejectProductSubmission(submissionId, request),
                http.getRequestURI()
        );
    }
}
