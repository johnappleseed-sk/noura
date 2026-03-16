package com.noura.platform.inventory.mapper;

import com.noura.platform.inventory.domain.Product;
import com.noura.platform.inventory.domain.ProductCategory;
import com.noura.platform.inventory.dto.category.CategorySummaryResponse;
import com.noura.platform.inventory.dto.product.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring")
public interface InventoryProductMapper {

    @Mapping(target = "categories", source = "productCategories")
    @Mapping(target = "primaryCategory", expression = "java(resolvePrimaryCategory(product))")
    ProductResponse toResponse(Product product);

    default CategorySummaryResponse map(ProductCategory productCategory) {
        return new CategorySummaryResponse(
                productCategory.getCategory().getId(),
                productCategory.getCategory().getCategoryCode(),
                productCategory.getCategory().getName(),
                productCategory.getCategory().getLevel(),
                productCategory.getCategory().isActive()
        );
    }

    default CategorySummaryResponse resolvePrimaryCategory(Product product) {
        return product.getProductCategories()
                .stream()
                .filter(ProductCategory::isPrimary)
                .findFirst()
                .map(this::map)
                .orElseGet(() -> product.getProductCategories()
                        .stream()
                        .sorted(Comparator.comparing(link -> link.getCategory().getName()))
                        .findFirst()
                        .map(this::map)
                        .orElse(null));
    }

    default List<CategorySummaryResponse> map(List<ProductCategory> productCategories) {
        return productCategories.stream()
                .sorted(Comparator
                        .comparing(ProductCategory::isPrimary)
                        .reversed()
                        .thenComparing(link -> link.getCategory().getName()))
                .map(this::map)
                .toList();
    }
}
