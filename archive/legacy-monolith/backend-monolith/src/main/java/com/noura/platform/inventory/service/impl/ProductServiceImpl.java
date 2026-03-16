package com.noura.platform.inventory.service.impl;

import com.noura.platform.common.exception.ApiException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.dto.recovery.RecoveryActionRequest;
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
import com.noura.platform.service.recovery.RecoveryGovernanceService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements governed inventory product administration workflows.
 */
@Service("inventoryProductServiceImpl")
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final InventoryProductRepository productRepository;
    private final InventoryCategoryRepository categoryRepository;
    private final InventoryProductMapper productMapper;
    private final RecoveryGovernanceService recoveryGovernanceService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public ProductResponse createProduct(ProductRequest request) {
        validateUniqueSku(request.sku(), null);
        Product product = new Product();
        applyProduct(product, request);
        Product saved = productRepository.save(product);
        syncCategories(saved, request);
        Product persisted = productRepository.save(saved);
        recoveryGovernanceService.captureVersion(
                "INVENTORY_PRODUCT",
                persisted.getId(),
                RecoveryActionType.CREATE,
                resolveActor(),
                "Inventory product created.",
                Map.of("source", "inventory-product-service")
        );
        return productMapper.toResponse(persisted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public ProductResponse updateProduct(String productId, ProductRequest request) {
        Product product = getProductEntity(productId);
        validateUniqueSku(request.sku(), productId);
        applyProduct(product, request);
        syncCategories(product, request);
        Product saved = productRepository.save(product);
        recoveryGovernanceService.captureVersion(
                "INVENTORY_PRODUCT",
                saved.getId(),
                RecoveryActionType.UPDATE,
                resolveActor(),
                "Inventory product updated.",
                Map.of("source", "inventory-product-service")
        );
        return productMapper.toResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public ProductResponse getProduct(String productId) {
        return productMapper.toResponse(getProductEntity(productId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public Page<ProductResponse> listProducts(ProductFilter filter, Pageable pageable) {
        ProductFilter effectiveFilter = filter == null ? new ProductFilter(null, null, null) : filter;
        return productRepository.findAll((root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.isNull(root.get("archivedAt")));
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
                predicates.add(cb.isNull(join.get("archivedAt")));
            }
            if (effectiveFilter.active() != null) {
                predicates.add(cb.equal(root.get("active"), effectiveFilter.active()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(productMapper::toResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public void deleteProduct(String productId) {
        recoveryGovernanceService.applyAction(
                new RecoveryActionRequest(
                        "INVENTORY_PRODUCT",
                        productId,
                        RecoveryActionType.TRASH,
                        "Inventory product moved to trash.",
                        null,
                        null,
                        null,
                        Map.of("source", "inventory-product-service")
                ),
                resolveActor()
        );
    }

    /**
     * Resolves a required inventory product aggregate that remains visible in the main workspace.
     *
     * @param productId The inventory product identifier.
     * @return The resolved inventory product aggregate.
     */
    private Product getProductEntity(String productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
                .filter(product -> product.getArchivedAt() == null)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
    }

    /**
     * Validates uniqueness for the inventory product SKU.
     *
     * @param sku The candidate product SKU.
     * @param productId The current product identifier when updating.
     */
    private void validateUniqueSku(String sku, String productId) {
        boolean exists = productId == null
                ? productRepository.existsBySkuIgnoreCaseAndDeletedAtIsNull(sku)
                : productRepository.existsBySkuIgnoreCaseAndDeletedAtIsNullAndIdNot(sku, productId);
        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_SKU_EXISTS", "Product SKU already exists");
        }
    }

    /**
     * Applies a request payload to an inventory product aggregate.
     *
     * @param product The inventory product aggregate.
     * @param request The request payload.
     */
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
            product.setArchivedAt(null);
        }
    }

    /**
     * Rebuilds the inventory product category assignments from the supplied request payload.
     *
     * @param product The inventory product aggregate.
     * @param request The request payload.
     */
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

    /**
     * Resolves governed inventory product categories from a request payload.
     *
     * @param categoryIds The requested category identifiers.
     * @return The resolved categories.
     */
    private List<Category> resolveCategories(List<String> categoryIds) {
        Set<String> uniqueIds = new LinkedHashSet<>(categoryIds);
        List<Category> categories = uniqueIds.stream()
                .map(id -> categoryRepository.findByIdAndDeletedAtIsNull(id)
                        .filter(category -> category.getArchivedAt() == null)
                        .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found: " + id)))
                .toList();
        if (categories.stream().anyMatch(category -> !category.isActive())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_INACTIVE", "Products can only be assigned to active categories");
        }
        return categories;
    }

    /**
     * Resolves the effective primary inventory product category.
     *
     * @param request The request payload.
     * @param categories The resolved product categories.
     * @return The primary category identifier.
     */
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

    /**
     * Normalizes optional string fields.
     *
     * @param value The raw string value.
     * @return The normalized value.
     */
    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * Resolves the current authenticated actor for recovery audit metadata.
     *
     * @return The resolved actor name.
     */
    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return "inventory-system";
        }
        return authentication.getName();
    }
}
