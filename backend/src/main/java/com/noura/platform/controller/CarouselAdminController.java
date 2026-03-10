package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.domain.enums.CarouselStatus;
import com.noura.platform.dto.carousel.CarouselBulkActionRequest;
import com.noura.platform.dto.carousel.CarouselPreviewDto;
import com.noura.platform.dto.carousel.CarouselPublishRequest;
import com.noura.platform.dto.carousel.CarouselReorderRequest;
import com.noura.platform.dto.carousel.CarouselSlideDto;
import com.noura.platform.dto.carousel.CarouselSlideRequest;
import com.noura.platform.dto.carousel.CarouselStatusUpdateRequest;
import com.noura.platform.service.CarouselService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "Carousel Admin")
@RequestMapping("${app.api.version-prefix:/api/v1}/admin/carousels")
public class CarouselAdminController {

    private final CarouselService carouselService;

    @Operation(summary = "List carousel slides")
    @GetMapping
    public ApiResponse<PageResponse<CarouselSlideDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CarouselStatus status,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String locale,
            @RequestParam(defaultValue = "false") Boolean includeDeleted,
            @RequestParam(required = false) Instant startFrom,
            @RequestParam(required = false) Instant startTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "position") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<CarouselSlideDto> response = carouselService.listAdminCarousels(
                q, status, published, storeId, locale, includeDeleted, startFrom, startTo, pageable
        );
        return ApiResponse.ok("Carousel slides", PageResponse.from(response), http.getRequestURI());
    }

    @Operation(summary = "Get carousel slide detail")
    @GetMapping("/{carouselId}")
    public ApiResponse<CarouselSlideDto> getById(@PathVariable UUID carouselId,
                                                 @RequestParam(defaultValue = "true") boolean includeDeleted,
                                                 HttpServletRequest http) {
        return ApiResponse.ok("Carousel slide", carouselService.getAdminCarousel(carouselId, includeDeleted), http.getRequestURI());
    }

    @Operation(summary = "Create carousel slide")
    @PostMapping
    public ResponseEntity<ApiResponse<CarouselSlideDto>> create(@Valid @RequestBody CarouselSlideRequest request,
                                                                HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Carousel slide created", carouselService.createCarousel(request), http.getRequestURI()));
    }

    @Operation(summary = "Update carousel slide")
    @PutMapping("/{carouselId}")
    public ApiResponse<CarouselSlideDto> update(@PathVariable UUID carouselId,
                                                @Valid @RequestBody CarouselSlideRequest request,
                                                HttpServletRequest http) {
        return ApiResponse.ok("Carousel slide updated", carouselService.updateCarousel(carouselId, request), http.getRequestURI());
    }

    @Operation(summary = "Soft delete carousel slide")
    @DeleteMapping("/{carouselId}")
    public ApiResponse<Void> delete(@PathVariable UUID carouselId, HttpServletRequest http) {
        carouselService.deleteCarousel(carouselId);
        return ApiResponse.ok("Carousel slide deleted", null, http.getRequestURI());
    }

    @Operation(summary = "Restore soft-deleted carousel slide")
    @PostMapping("/{carouselId}/restore")
    public ApiResponse<CarouselSlideDto> restore(@PathVariable UUID carouselId, HttpServletRequest http) {
        return ApiResponse.ok("Carousel slide restored", carouselService.restoreCarousel(carouselId), http.getRequestURI());
    }

    @Operation(summary = "Change carousel slide status")
    @PatchMapping("/{carouselId}/status")
    public ApiResponse<CarouselSlideDto> updateStatus(@PathVariable UUID carouselId,
                                                      @Valid @RequestBody CarouselStatusUpdateRequest request,
                                                      HttpServletRequest http) {
        return ApiResponse.ok("Carousel status updated", carouselService.changeStatus(carouselId, request.status()), http.getRequestURI());
    }

    @Operation(summary = "Publish or unpublish carousel slide")
    @PatchMapping("/{carouselId}/publish")
    public ApiResponse<CarouselSlideDto> publish(@PathVariable UUID carouselId,
                                                 @Valid @RequestBody CarouselPublishRequest request,
                                                 HttpServletRequest http) {
        return ApiResponse.ok(
                "Carousel publish state updated",
                carouselService.publishCarousel(carouselId, Boolean.TRUE.equals(request.published()), request.startAt(), request.endAt()),
                http.getRequestURI()
        );
    }

    @Operation(summary = "Reorder carousel slides")
    @PatchMapping("/reorder")
    public ApiResponse<List<CarouselSlideDto>> reorder(@Valid @RequestBody CarouselReorderRequest request,
                                                       HttpServletRequest http) {
        return ApiResponse.ok("Carousel order updated", carouselService.reorderCarousels(request.items()), http.getRequestURI());
    }

    @Operation(summary = "Duplicate carousel slide")
    @PostMapping("/{carouselId}/duplicate")
    public ResponseEntity<ApiResponse<CarouselSlideDto>> duplicate(@PathVariable UUID carouselId, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Carousel slide duplicated", carouselService.duplicateCarousel(carouselId), http.getRequestURI()));
    }

    @Operation(summary = "Preview storefront rendering rules for carousel slide")
    @GetMapping("/{carouselId}/preview")
    public ApiResponse<CarouselPreviewDto> preview(@PathVariable UUID carouselId, HttpServletRequest http) {
        return ApiResponse.ok("Carousel preview", carouselService.previewCarousel(carouselId), http.getRequestURI());
    }

    @Operation(summary = "Apply bulk action to carousel slides")
    @PostMapping("/bulk-action")
    public ApiResponse<List<CarouselSlideDto>> bulkAction(@Valid @RequestBody CarouselBulkActionRequest request,
                                                          HttpServletRequest http) {
        return ApiResponse.ok("Carousel bulk action applied", carouselService.applyBulkAction(request), http.getRequestURI());
    }
}
