package com.noura.platform.inventory.api;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.inventory.dto.product.ProductFilter;
import com.noura.platform.inventory.dto.product.ProductRequest;
import com.noura.platform.inventory.dto.product.ProductResponse;
import com.noura.platform.inventory.service.ProductService;
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

import java.util.Set;

@Validated
@RestController("inventoryProductController")
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1/products")
public class ProductController {

    private static final Set<String> ALLOWED_SORTS = Set.of("name", "sku", "basePrice", "createdAt", "updatedAt");

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<PageResponse<ProductResponse>> listProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = InventoryPageRequestFactory.of(page, size, sortBy, direction, ALLOWED_SORTS, "createdAt");
        Page<ProductResponse> response = productService.listProducts(new ProductFilter(query, categoryId, active), pageable);
        return ApiResponse.ok("Products", PageResponse.from(response), http.getRequestURI());
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ApiResponse<ProductResponse> getProduct(@PathVariable String productId, HttpServletRequest http) {
        return ApiResponse.ok("Product", productService.getProduct(productId), http.getRequestURI());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product created", productService.createProduct(request), http.getRequestURI()));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable String productId,
            @Valid @RequestBody ProductRequest request,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Product updated", productService.updateProduct(productId, request), http.getRequestURI());
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteProduct(@PathVariable String productId, HttpServletRequest http) {
        productService.deleteProduct(productId);
        return ApiResponse.ok("Product deleted", null, http.getRequestURI());
    }
}
