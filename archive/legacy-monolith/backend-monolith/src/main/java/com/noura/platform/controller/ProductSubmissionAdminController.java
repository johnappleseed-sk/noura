package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.domain.enums.ProductSubmissionStatus;
import com.noura.platform.domain.enums.ProductSubmissionReviewAction;
import com.noura.platform.domain.enums.SubmissionStatus;
import com.noura.platform.dto.product.ProductRequest;
import com.noura.platform.dto.submission.ProductSubmissionDecisionRequest;
import com.noura.platform.dto.submission.ProductSubmissionDetailDto;
import com.noura.platform.dto.submission.ProductSubmissionDto;
import com.noura.platform.dto.submission.ProductSubmissionReviewDto;
import com.noura.platform.dto.superinventory.ApproveProductSubmissionRequest;
import com.noura.platform.dto.superinventory.ProductApprovalDecisionResponse;
import com.noura.platform.dto.superinventory.ProductSubmissionDetailResponse;
import com.noura.platform.dto.superinventory.ProductSubmissionResponse;
import com.noura.platform.dto.superinventory.RejectProductSubmissionRequest;
import com.noura.platform.repository.ProductSubmissionRepository;
import com.noura.platform.service.ProductSubmissionService;
import com.noura.platform.service.SuperInventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/admin/product-submissions")
public class ProductSubmissionAdminController {

    private final ProductSubmissionService productSubmissionService;
    private final SuperInventoryService superInventoryService;
    private final ProductSubmissionRepository productSubmissionRepository;

    @GetMapping
    public ApiResponse<PageResponse<ProductSubmissionDto>> list(
            @RequestParam(required = false) ProductSubmissionStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean duplicatesOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<ProductSubmissionDto> submissions = status == ProductSubmissionStatus.REVISION_REQUESTED
                ? Page.empty(pageable)
                : superInventoryService
                .listProductSubmissions(toCanonicalStatus(status), null, null, query, pageable)
                .map(this::toLegacyDto);
        return ApiResponse.ok("Product submissions", PageResponse.from(submissions), http.getRequestURI());
    }

    @GetMapping("/{submissionId}")
    public ApiResponse<ProductSubmissionDetailDto> get(@PathVariable UUID submissionId, HttpServletRequest http) {
        ProductSubmissionDetailResponse response = superInventoryService.getProductSubmission(submissionId);
        return ApiResponse.ok("Product submission", toLegacyDetailDto(response), http.getRequestURI());
    }

    @PostMapping("/{submissionId}/approve")
    public ApiResponse<ProductSubmissionDto> approve(
            @PathVariable UUID submissionId,
            @Valid @RequestBody ProductSubmissionDecisionRequest request,
            HttpServletRequest http
    ) {
        ApproveProductSubmissionRequest approveRequest = new ApproveProductSubmissionRequest(
                request.existingMasterProductId(),
                request.note()
        );
        ProductSubmissionResponse response = superInventoryService.approveProductSubmission(submissionId, approveRequest);
        return ApiResponse.ok("Submission approved", toLegacyDto(response), http.getRequestURI());
    }

    @PostMapping("/{submissionId}/reject")
    public ApiResponse<ProductSubmissionDto> reject(
            @PathVariable UUID submissionId,
            @Valid @RequestBody ProductSubmissionDecisionRequest request,
            HttpServletRequest http
    ) {
        RejectProductSubmissionRequest rejectRequest = new RejectProductSubmissionRequest(request.note());
        ProductSubmissionResponse response = superInventoryService.rejectProductSubmission(submissionId, rejectRequest);
        return ApiResponse.ok("Submission rejected", toLegacyDto(response), http.getRequestURI());
    }

    @PostMapping("/{submissionId}/request-revision")
    public ApiResponse<ProductSubmissionDto> requestRevision(
            @PathVariable UUID submissionId,
            @Valid @RequestBody ProductSubmissionDecisionRequest request,
            HttpServletRequest http
    ) {
        if (productSubmissionRepository.existsById(submissionId)) {
            throw new BadRequestException(
                    "REQUEST_REVISION_UNSUPPORTED",
                    "request-revision is only supported for legacy submission workflow"
            );
        }
        return ApiResponse.ok("Revision requested", productSubmissionService.requestRevision(submissionId, request), http.getRequestURI());
    }

    @PostMapping("/{submissionId}/link-duplicate")
    public ApiResponse<ProductSubmissionDto> linkDuplicate(
            @PathVariable UUID submissionId,
            @Valid @RequestBody ProductSubmissionDecisionRequest request,
            HttpServletRequest http
    ) {
        if (request.existingMasterProductId() == null) {
            throw new BadRequestException("MASTER_PRODUCT_REQUIRED", "existingMasterProductId is required to link duplicate");
        }
        ApproveProductSubmissionRequest approveRequest = new ApproveProductSubmissionRequest(
                request.existingMasterProductId(),
                request.note()
        );
        ProductSubmissionResponse response = superInventoryService.approveProductSubmission(submissionId, approveRequest);
        return ApiResponse.ok("Duplicate linked", toLegacyDto(response), http.getRequestURI());
    }

    private SubmissionStatus toCanonicalStatus(ProductSubmissionStatus legacyStatus) {
        if (legacyStatus == null) {
            return null;
        }
        return switch (legacyStatus) {
            case PENDING_REVIEW -> SubmissionStatus.PENDING_REVIEW;
            case APPROVED -> SubmissionStatus.APPROVED;
            case REJECTED -> SubmissionStatus.REJECTED;
            case REVISION_REQUESTED -> null;
        };
    }

    private ProductSubmissionStatus toLegacyStatus(SubmissionStatus status) {
        if (status == null) {
            return ProductSubmissionStatus.PENDING_REVIEW;
        }
        return switch (status) {
            case PENDING_REVIEW -> ProductSubmissionStatus.PENDING_REVIEW;
            case APPROVED -> ProductSubmissionStatus.APPROVED;
            case REJECTED -> ProductSubmissionStatus.REJECTED;
        };
    }

    private ProductSubmissionDto toLegacyDto(ProductSubmissionResponse response) {
        return new ProductSubmissionDto(
                response.id(),
                response.storeId(),
                null,
                response.merchantId(),
                null,
                1,
                toLegacyStatus(response.status()),
                false,
                response.targetProductId(),
                null,
                firstNonNull(response.submittedAt(), response.reviewedAt()),
                response.reviewedAt()
        );
    }

    private ProductSubmissionDetailDto toLegacyDetailDto(ProductSubmissionDetailResponse response) {
        ProductRequest productRequest = new ProductRequest(
                response.proposedName(),
                null,
                null,
                response.proposedCategoryCode(),
                response.proposedBrand(),
                null,
                response.proposedBarcode(),
                null,
                response.proposedAttributesJson(),
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        List<ProductSubmissionReviewDto> reviews = response.decisions() == null
                ? List.of()
                : response.decisions().stream().map(this::toLegacyReview).toList();

        return new ProductSubmissionDetailDto(
                response.id(),
                response.storeId(),
                null,
                response.merchantId(),
                null,
                null,
                1,
                toLegacyStatus(response.status()),
                productRequest,
                null,
                false,
                response.targetProductId(),
                null,
                response.reviewedBy(),
                response.reviewedAt(),
                response.reviewNotes(),
                List.of(),
                reviews,
                firstNonNull(response.submittedAt(), response.createdAt(), response.updatedAt())
        );
    }

    private ProductSubmissionReviewDto toLegacyReview(ProductApprovalDecisionResponse decision) {
        return new ProductSubmissionReviewDto(
                decision.id(),
                toLegacyReviewAction(decision),
                decision.decidedBy(),
                decision.notes(),
                decision.targetProductId(),
                decision.decidedAt()
        );
    }

    private ProductSubmissionReviewAction toLegacyReviewAction(ProductApprovalDecisionResponse decision) {
        return switch (decision.decisionType()) {
            case APPROVED -> ProductSubmissionReviewAction.APPROVED;
            case REJECTED -> ProductSubmissionReviewAction.REJECTED;
        };
    }

    private Instant firstNonNull(Instant... instants) {
        for (Instant instant : instants) {
            if (instant != null) {
                return instant;
            }
        }
        return null;
    }
}
