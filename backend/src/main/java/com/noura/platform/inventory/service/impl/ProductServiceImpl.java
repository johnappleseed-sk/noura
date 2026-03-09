package com.noura.platform.inventory.service.impl;

import com.noura.platform.common.exception.ApiException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.inventory.domain.Category;
import com.noura.platform.inventory.domain.Product;
import com.noura.platform.inventory.domain.ProductCategory;
import com.noura.platform.inventory.domain.id.ProductCategoryId;
import com.noura.platform.inventory.dto.product.ProductFilter;
import com.noura.platform.inventory.dto.product.ProductRequest;
import com.noura.platform.inventory.dto.product.ProductResponse;
import com.noura.platform.inventory.mapper.InventoryProductMapper;
import com.noura.platform.inventory.repository.InventoryCategoryRepository;
import com.noura.platform.inventory.repository.InventoryProductRepository;
import com.noura.platform.inventory.service.ProductService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service("inventoryProductServiceImpl")
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final InventoryProductRepository productRepository;
    private final InventoryCategoryRepository categoryRepository;
    private final InventoryProductMapper productMapper;

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public ProductResponse createProduct(ProductRequest request) {
        validateUniqueSku(request.sku(), null);
        Product product = new Product();
        applyProduct(product, request);
        Product saved = productRepository.save(product);
        syncCategories(saved, request);
        return productMapper.toResponse(productRepository.save(saved));
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public ProductResponse updateProduct(String productId, ProductRequest request) {
        Product product = getProductEntity(productId);
        validateUniqueSku(request.sku(), productId);
        applyProduct(product, request);
        syncCategories(product, request);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public ProductResponse getProduct(String productId) {
        return productMapper.toResponse(getProductEntity(productId));
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public Page<ProductResponse> listProducts(ProductFilter filter, Pageable pageable) {
        ProductFilter effectiveFilter = filter == null ? new ProductFilter(null, null, null) : filter;
        return productRepository.findAll((root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (StringUtils.hasText(effectiveFilter.query())) {
                String likeValue = "%" + effectiveFilter.query().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likeValue),
                        cb.like(cb.lower(root.get("sku")), likeValue),
                        cb.like(cb.lower(root.get("barcodeValue")), likeValue)
                ));
            }
            if (StringUtils.hasText(effectiveFilter.categoryId())) {
                var join = root.join("productCategories", JoinType.INNER).join("category", JoinType.INNER);
                predicates.add(cb.equal(join.get("id"), effectiveFilter.categoryId()));
                predicates.add(cb.isNull(join.get("deletedAt")));
            }
            if (effectiveFilter.active() != null) {
                predicates.add(cb.equal(root.get("active"), effectiveFilter.active()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public void deleteProduct(String productId) {
        Product product = getProductEntity(productId);
        product.setActive(false);
        product.setDeletedAt(Instant.now());
        productRepository.save(product);
    }

    private Product getProductEntity(String productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
    }

    private void validateUniqueSku(String sku, String productId) {
        boolean exists = productId == null
                ? productRepository.existsBySkuIgnoreCaseAndDeletedAtIsNull(sku)
                : productRepository.existsBySkuIgnoreCaseAndDeletedAtIsNullAndIdNot(sku, productId);
        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_SKU_EXISTS", "Product SKU already exists");
        }
    }

    private void applyProduct(Product product, ProductRequest request) {
        product.setSku(request.sku().trim());
        product.setName(request.name().trim());
        product.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        product.setStatus(request.status().trim().toUpperCase());
        product.setBasePrice(request.basePrice());
        product.setCurrencyCode(request.currencyCode().trim().toUpperCase());
        product.setWidthCm(request.widthCm());
        product.setHeightCm(request.heightCm());
        product.setLengthCm(request.lengthCm());
        product.setWeightKg(request.weightKg());
        product.setBatchTracked(Boolean.TRUE.equals(request.batchTracked()));
        product.setSerialTracked(Boolean.TRUE.equals(request.serialTracked()));
        product.setBarcodeValue(normalizeNullable(request.barcodeValue()));
        product.setQrCodeValue(normalizeNullable(request.qrCodeValue()));
        product.setActive(request.active() == null || request.active());
        if (product.isActive()) {
            product.setDeletedAt(null);
        }
    }

    private void syncCategories(Product product, ProductRequest request) {
        List<Category> categories = resolveCategories(request.categoryIds());
        String primaryCategoryId = resolvePrimaryCategoryId(request, categories);
        product.getProductCategories().clear();
        for (Category category : categories) {
            ProductCategory link = new ProductCategory();
            link.setId(new ProductCategoryId(product.getId(), category.getId()));
            link.setProduct(product);
            link.setCategory(category);
            link.setPrimary(category.getId().equals(primaryCategoryId));
            product.getProductCategories().add(link);
        }
    }

    private List<Category> resolveCategories(List<String> categoryIds) {
        Set<String> uniqueIds = new LinkedHashSet<>(categoryIds);
        List<Category> categories = uniqueIds.stream()
                .map(id -> categoryRepository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found: " + id)))
                .toList();
        if (categories.stream().anyMatch(category -> !category.isActive())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_INACTIVE", "Products can only be assigned to active categories");
        }
        return categories;
    }

    private String resolvePrimaryCategoryId(ProductRequest request, List<Category> categories) {
        if (StringUtils.hasText(request.primaryCategoryId())) {
            boolean exists = categories.stream().anyMatch(category -> category.getId().equals(request.primaryCategoryId()));
            if (!exists) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "PRIMARY_CATEGORY_INVALID",
                        "Primary category must be included in categoryIds"
                );
            }
            return request.primaryCategoryId();
        }
        return categories.getFirst().getId();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
