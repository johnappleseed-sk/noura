package com.noura.platform.inventory.api;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.inventory.dto.category.CategoryFilter;
import com.noura.platform.inventory.dto.category.CategoryRequest;
import com.noura.platform.inventory.dto.category.CategoryResponse;
import com.noura.platform.inventory.dto.category.CategoryTreeResponse;
import com.noura.platform.inventory.service.CategoryService;
import com.noura.platform.inventory.support.InventoryPageRequestFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1/categories")
public class CategoryController {

    private static final Set<String> ALLOWED_SORTS = Set.of("name", "categoryCode", "sortOrder", "level", "createdAt", "updatedAt");

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<PageResponse<CategoryResponse>> listCategories(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String parentId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = InventoryPageRequestFactory.of(page, size, sortBy, direction, ALLOWED_SORTS, "sortOrder");
        Page<CategoryResponse> response = categoryService.listCategories(new CategoryFilter(query, parentId, active), pageable);
        return ApiResponse.ok("Categories", PageResponse.from(response), http.getRequestURI());
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<List<CategoryTreeResponse>> categoryTree(
            @RequestParam(defaultValue = "true") Boolean activeOnly,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Category tree", categoryService.getCategoryTree(activeOnly), http.getRequestURI());
    }

    @GetMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<CategoryResponse> getCategory(@PathVariable String categoryId, HttpServletRequest http) {
        return ApiResponse.ok("Category", categoryService.getCategory(categoryId), http.getRequestURI());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Category created", categoryService.createCategory(request), http.getRequestURI()));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable String categoryId,
            @Valid @RequestBody CategoryRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Category updated", categoryService.updateCategory(categoryId, request), http.getRequestURI());
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteCategory(@PathVariable String categoryId, HttpServletRequest http) {
        categoryService.deleteCategory(categoryId);
        return ApiResponse.ok("Category deleted", null, http.getRequestURI());
    }
}
