package com.noura.platform.commerce.catalog.application;

import com.noura.platform.commerce.api.v1.dto.inventory.StockAvailabilityDto;
import com.noura.platform.commerce.api.v1.service.ApiInventoryService;
import com.noura.platform.commerce.entity.Category;
import com.noura.platform.commerce.entity.Product;
import com.noura.platform.commerce.entity.ProductUnit;
import com.noura.platform.commerce.catalog.web.StorefrontCategoryDto;
import com.noura.platform.commerce.catalog.web.StorefrontProductCardDto;
import com.noura.platform.commerce.catalog.web.StorefrontProductDetailDto;
import com.noura.platform.commerce.catalog.web.StorefrontProductUnitDto;
import com.noura.platform.commerce.repository.CategoryRepo;
import com.noura.platform.commerce.repository.ProductRepo;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class StorefrontCatalogService {
    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;
    private final ApiInventoryService apiInventoryService;

    public StorefrontCatalogService(ProductRepo productRepo,
                                    CategoryRepo categoryRepo,
                                    ApiInventoryService apiInventoryService) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.apiInventoryService = apiInventoryService;
    }

    public List<StorefrontCategoryDto> listCategories() {
        Map<Long, Long> productCounts = new LinkedHashMap<>();
        for (ProductRepo.CategoryCount count : productRepo.countByCategory()) {
            if (count.getCategoryId() != null) {
                productCounts.put(count.getCategoryId(), count.getCount() == null ? 0L : count.getCount());
            }
        }

        return categoryRepo.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("name")))
                .stream()
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .map(category -> new StorefrontCategoryDto(
                        category.getId(),
                        category.getName(),
                        category.getDescription(),
                        category.getImageUrl(),
                        category.getSortOrder(),
                        productCounts.getOrDefault(category.getId(), 0L)
                ))
                .toList();
    }

    public Page<StorefrontProductCardDto> listProducts(String q, Long categoryId, Pageable pageable) {
        return productRepo.findAll(activeCatalogSpec(q, categoryId), pageable)
                .map(this::toProductCard);
    }

    public StorefrontProductDetailDto getProduct(Long productId) {
        Product product = productRepo.findById(productId)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        return toProductDetail(product);
    }

    public StockAvailabilityDto getAvailability(Long productId) {
        Product product = productRepo.findById(productId)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        return apiInventoryService.getAvailability(product.getId());
    }

    private Specification<Product> activeCatalogSpec(String q, Long categoryId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (categoryId != null) {
                predicates.add(cb.equal(root.join("category", JoinType.LEFT).get("id"), categoryId));
            }

            String normalizedQuery = q == null ? null : q.trim().toLowerCase();
            if (normalizedQuery != null && !normalizedQuery.isEmpty()) {
                String like = "%" + normalizedQuery + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("sku"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("barcode"), "")), like)
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private StorefrontProductCardDto toProductCard(Product product) {
        Category category = product.getCategory();
        return new StorefrontProductCardDto(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                product.getStockQty(),
                product.isLowStock(),
                Boolean.TRUE.equals(product.getAllowNegativeStock())
        );
    }

    private StorefrontProductDetailDto toProductDetail(Product product) {
        Category category = product.getCategory();
        List<StorefrontProductUnitDto> units = product.getProductUnits() == null
                ? List.of()
                : product.getProductUnits().stream()
                .map(this::toProductUnit)
                .toList();

        return new StorefrontProductDetailDto(
                product.getId(),
                product.getSku(),
                product.getBarcode(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                product.getStockQty(),
                product.getLowStockThreshold(),
                Boolean.TRUE.equals(product.getActive()),
                Boolean.TRUE.equals(product.getAllowNegativeStock()),
                product.getBaseUnitName(),
                product.getBaseUnitPrecision(),
                product.getBoxSpecifications(),
                product.getWeightValue(),
                product.getWeightUnit(),
                product.getLengthValue(),
                product.getLengthUnit(),
                product.getWidthValue(),
                product.getWidthUnit(),
                product.getHeightValue(),
                product.getHeightUnit(),
                units
        );
    }

    private StorefrontProductUnitDto toProductUnit(ProductUnit unit) {
        return new StorefrontProductUnitDto(
                unit.getId(),
                unit.getName(),
                unit.getAbbreviation(),
                unit.getConversionToBase(),
                Boolean.TRUE.equals(unit.getIsDefaultSaleUnit())
        );
    }
}
