package com.noura.platform.service.impl.recovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.enums.RecoveryLifecycleState;
import com.noura.platform.inventory.domain.Category;
import com.noura.platform.inventory.domain.Product;
import com.noura.platform.inventory.domain.ProductCategory;
import com.noura.platform.inventory.domain.id.ProductCategoryId;
import com.noura.platform.inventory.repository.InventoryCategoryRepository;
import com.noura.platform.inventory.repository.InventoryProductRepository;
import com.noura.platform.service.recovery.RecoverableEntityAdapter;
import com.noura.platform.service.recovery.RecoverableEntityHandle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * Implements recovery governance behavior for inventory products.
 */
@Component
@ConditionalOnProperty(prefix = "inventory", name = "enabled", havingValue = "true")
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${inventory.datasource.url:}')")
public class InventoryProductRecoveryAdapter implements RecoverableEntityAdapter {
    private static final String ENTITY_TYPE = "INVENTORY_PRODUCT";

    private final InventoryProductRepository productRepository;
    private final InventoryCategoryRepository categoryRepository;

    /**
     * Creates a new inventory product recovery adapter.
     *
     * @param productRepository The inventory product repository.
     * @param categoryRepository The inventory category repository.
     */
    public InventoryProductRecoveryAdapter(
            InventoryProductRepository productRepository,
            InventoryCategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEntityType() {
        return ENTITY_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RecoverableEntityHandle> findHandle(String entityId) {
        return productRepository.findById(requireEntityId(entityId)).map(ProductHandle::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecoverableEntityHandle restoreHandle(String entityId, JsonNode snapshot) {
        String normalizedEntityId = requireEntityId(entityId);
        Product product = productRepository.findById(normalizedEntityId).orElseGet(Product::new);
        product.setId(normalizedEntityId);
        ProductHandle handle = new ProductHandle(product);
        handle.restoreFromSnapshot(snapshot, RecoveryLifecycleState.ACTIVE);
        return handle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hardDelete(String entityId) {
        Product product = productRepository.findById(requireEntityId(entityId))
                .orElseThrow(() -> new NotFoundException("INVENTORY_PRODUCT_NOT_FOUND", "Inventory product not found."));
        productRepository.delete(product);
    }

    /**
     * Normalizes and validates a governed entity identifier.
     *
     * @param entityId The raw governed entity identifier.
     * @return The normalized identifier.
     */
    private String requireEntityId(String entityId) {
        if (!StringUtils.hasText(entityId)) {
            throw new BadRequestException("RECOVERY_ENTITY_ID_INVALID", "Inventory product identifier is required.");
        }
        return entityId.trim();
    }

    /**
     * Provides a recoverable handle over an inventory product aggregate.
     */
    private final class ProductHandle implements RecoverableEntityHandle {
        private final Product product;

        /**
         * Creates a new inventory product handle.
         *
         * @param product The inventory product aggregate.
         */
        private ProductHandle(Product product) {
            this.product = product;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getEntityId() {
            return product.getId();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return StringUtils.hasText(product.getName()) ? product.getName() : product.getSku();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object toSnapshot() {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", product.getId());
            snapshot.put("sku", product.getSku());
            snapshot.put("name", product.getName());
            snapshot.put("description", product.getDescription());
            snapshot.put("status", product.getStatus());
            snapshot.put("basePrice", product.getBasePrice());
            snapshot.put("currencyCode", product.getCurrencyCode());
            snapshot.put("widthCm", product.getWidthCm());
            snapshot.put("heightCm", product.getHeightCm());
            snapshot.put("lengthCm", product.getLengthCm());
            snapshot.put("weightKg", product.getWeightKg());
            snapshot.put("batchTracked", product.isBatchTracked());
            snapshot.put("serialTracked", product.isSerialTracked());
            snapshot.put("barcodeValue", product.getBarcodeValue());
            snapshot.put("qrCodeValue", product.getQrCodeValue());
            snapshot.put("active", product.isActive());
            snapshot.put("deletedAt", product.getDeletedAt());
            snapshot.put("archivedAt", product.getArchivedAt());
            snapshot.put("categoryIds", product.getProductCategories().stream().map(link -> link.getCategory().getId()).toList());
            snapshot.put("primaryCategoryId", product.getProductCategories().stream()
                    .filter(ProductCategory::isPrimary)
                    .map(link -> link.getCategory().getId())
                    .findFirst()
                    .orElse(null));
            return snapshot;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void applyLifecycleState(RecoveryLifecycleState state) {
            switch (state) {
                case ACTIVE -> {
                    product.setActive(true);
                    product.setArchivedAt(null);
                    product.setDeletedAt(null);
                }
                case INACTIVE -> {
                    product.setActive(false);
                    product.setArchivedAt(null);
                    product.setDeletedAt(null);
                }
                case ARCHIVED -> {
                    product.setActive(false);
                    product.setArchivedAt(Instant.now());
                    product.setDeletedAt(null);
                }
                case TRASHED -> {
                    product.setActive(false);
                    product.setArchivedAt(null);
                    product.setDeletedAt(Instant.now());
                }
                case PURGED, ANONYMIZED -> {
                    product.setActive(false);
                    product.setArchivedAt(product.getArchivedAt() == null ? Instant.now() : product.getArchivedAt());
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void restoreFromSnapshot(JsonNode snapshot, RecoveryLifecycleState targetState) {
            product.setSku(text(snapshot, "sku"));
            product.setName(text(snapshot, "name"));
            product.setDescription(text(snapshot, "description"));
            product.setStatus(text(snapshot, "status"));
            product.setBasePrice(decimal(snapshot, "basePrice"));
            product.setCurrencyCode(text(snapshot, "currencyCode"));
            product.setWidthCm(decimal(snapshot, "widthCm"));
            product.setHeightCm(decimal(snapshot, "heightCm"));
            product.setLengthCm(decimal(snapshot, "lengthCm"));
            product.setWeightKg(decimal(snapshot, "weightKg"));
            product.setBatchTracked(bool(snapshot, "batchTracked"));
            product.setSerialTracked(bool(snapshot, "serialTracked"));
            product.setBarcodeValue(text(snapshot, "barcodeValue"));
            product.setQrCodeValue(text(snapshot, "qrCodeValue"));
            syncCategories(snapshot);
            applyLifecycleState(targetState);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void persist() {
            productRepository.save(product);
        }

        /**
         * Recreates product-category links from the stored snapshot.
         *
         * @param snapshot The serialized snapshot payload.
         */
        private void syncCategories(JsonNode snapshot) {
            List<Category> categories = resolveCategories(snapshot);
            String primaryCategoryId = text(snapshot, "primaryCategoryId");
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
         * Resolves product categories from a stored snapshot payload.
         *
         * @param snapshot The serialized snapshot payload.
         * @return The resolved product categories.
         */
        private List<Category> resolveCategories(JsonNode snapshot) {
            JsonNode categoryIds = snapshot.get("categoryIds");
            if (categoryIds == null || !categoryIds.isArray()) {
                return List.of();
            }
            List<Category> categories = new ArrayList<>();
            StreamSupport.stream(categoryIds.spliterator(), false)
                    .map(JsonNode::asText)
                    .filter(StringUtils::hasText)
                    .forEach(categoryId -> categories.add(
                            categoryRepository.findById(categoryId)
                                    .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Inventory product category not found for restore."))
                    ));
            return categories;
        }

        /**
         * Reads a text field from an inventory product snapshot.
         *
         * @param snapshot The serialized snapshot payload.
         * @param fieldName The requested field name.
         * @return The resolved text value.
         */
        private String text(JsonNode snapshot, String fieldName) {
            JsonNode field = snapshot.get(fieldName);
            return field == null || field.isNull() ? null : field.asText();
        }

        /**
         * Reads a decimal field from an inventory product snapshot.
         *
         * @param snapshot The serialized snapshot payload.
         * @param fieldName The requested field name.
         * @return The resolved decimal value.
         */
        private BigDecimal decimal(JsonNode snapshot, String fieldName) {
            JsonNode field = snapshot.get(fieldName);
            return field == null || field.isNull() ? null : field.decimalValue();
        }

        /**
         * Reads a boolean field from an inventory product snapshot.
         *
         * @param snapshot The serialized snapshot payload.
         * @param fieldName The requested field name.
         * @return The resolved boolean value.
         */
        private boolean bool(JsonNode snapshot, String fieldName) {
            JsonNode field = snapshot.get(fieldName);
            return field != null && !field.isNull() && field.asBoolean();
        }
    }
}
