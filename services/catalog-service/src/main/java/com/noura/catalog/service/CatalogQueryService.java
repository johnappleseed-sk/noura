package com.noura.catalog.service;

import com.noura.catalog.dto.catalog.CategoryTreeDto;
import com.noura.catalog.dto.product.ProductDto;
import com.noura.catalog.dto.product.ProductSearchResultDto;
import com.noura.catalog.dto.product.ProductStoreInventoryDto;
import com.noura.catalog.dto.product.TrendTagDto;
import com.noura.catalog.domain.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CatalogQueryService {

    Page<ProductDto> listProducts(
            String query,
            String category,
            UUID categoryId,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            Boolean flashSale,
            Boolean trending,
            Pageable pageable
    );

    ProductDto getProduct(UUID productId);

    List<ProductStoreInventoryDto> productInventory(UUID productId);

    List<CategoryTreeDto> categoryTree();

    List<ProductSearchResultDto> searchProducts(String query);

    Page<ProductSearchResultDto> searchProducts(
            String keyword,
            UUID categoryId,
            UUID brandId,
            ProductStatus status,
            Pageable pageable
    );

    List<TrendTagDto> trendTags();
}
