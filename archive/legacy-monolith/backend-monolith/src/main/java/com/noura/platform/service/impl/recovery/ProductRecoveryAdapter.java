package com.noura.platform.service.impl.recovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Brand;
import com.noura.platform.domain.entity.Category;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.enums.ProductStatus;
import com.noura.platform.domain.enums.RecoveryLifecycleState;
import com.noura.platform.repository.BrandRepository;
import com.noura.platform.repository.CategoryRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.service.recovery.RecoverableEntityAdapter;
import com.noura.platform.service.recovery.RecoverableEntityHandle;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements recoverable-entity behavior for commerce products.
 */
@Component
public class ProductRecoveryAdapter implements RecoverableEntityAdapter {
    private static final String ENTITY_TYPE = "PRODUCT";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    /**
     * Creates a new product recovery adapter.
     *
     * @param productRepository The product repository.
     * @param categoryRepository The category repository.
     * @param brandRepository The brand repository.
     */
    public ProductRecoveryAdapter(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
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
        UUID productId = parseUuid(entityId);
        return productRepository.findById(productId).map(ProductHandle::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecoverableEntityHandle restoreHandle(String entityId, JsonNode snapshot) {
        UUID productId = parseUuid(entityId);
        Product product = productRepository.findById(productId).orElseGet(Product::new);
        product.setId(productId);
        ProductHandle handle = new ProductHandle(product);
        handle.restoreFromSnapshot(snapshot, RecoveryLifecycleState.ACTIVE);
        return handle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hardDelete(String entityId) {
        UUID productId = parseUuid(entityId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        productRepository.delete(product);
    }

    /**
     * Parses and validates a governed product identifier.
     *
     * @param entityId The governed entity identifier.
     * @return The parsed UUID.
     */
    private UUID parseUuid(String entityId) {
        try {
            return UUID.fromString(entityId);
        } catch (RuntimeException exception) {
            throw new BadRequestException("RECOVERY_ENTITY_ID_INVALID", "Invalid product identifier: " + entityId);
        }
    }

    /**
     * Provides a recoverable-entity handle over a commerce product aggregate.
     */
    private final class ProductHandle implements RecoverableEntityHandle {
        private final Product product;

        /**
         * Creates a new handle over a product aggregate.
         *
         * @param product The product aggregate.
         */
        private ProductHandle(Product product) {
            this.product = product;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getEntityId() {
            return String.valueOf(product.getId());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            if (StringUtils.hasText(product.getName())) {
                return product.getName();
            }
            return String.valueOf(product.getId());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object toSnapshot() {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", product.getId());
            snapshot.put("name", product.getName());
            snapshot.put("categoryId", product.getCategory() == null ? null : product.getCategory().getId());
            snapshot.put("brandId", product.getBrand() == null ? null : product.getBrand().getId());
            snapshot.put("basePrice", product.getBasePrice());
            snapshot.put("attributes", product.getAttributes());
            snapshot.put("status", product.getStatus());
            snapshot.put("active", product.isActive());
            snapshot.put("allowBackorder", product.isAllowBackorder());
            snapshot.put("deletedAt", product.getDeletedAt());
            snapshot.put("flashSale", product.isFlashSale());
            snapshot.put("trending", product.isTrending());
            snapshot.put("bestSeller", product.isBestSeller());
            snapshot.put("averageRating", product.getAverageRating());
            snapshot.put("reviewCount", product.getReviewCount());
            snapshot.put("popularityScore", product.getPopularityScore());
            snapshot.put("shortDescription", product.getShortDescription());
            snapshot.put("longDescription", product.getLongDescription());
            snapshot.put("targetAudience", product.getTargetAudience());
            snapshot.put("barcode", product.getBarcode());
            snapshot.put("qrCode", product.getQrCode());
            snapshot.put("seoTitle", product.getSeoTitle());
            snapshot.put("seoDescription", product.getSeoDescription());
            snapshot.put("seoSlug", product.getSeoSlug());
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
                    product.setDeletedAt(null);
                }
                case TRASHED -> {
                    product.setActive(false);
                    product.setDeletedAt(Instant.now());
                }
                case INACTIVE, ARCHIVED, PURGED, ANONYMIZED -> {
                    product.setActive(false);
                    product.setDeletedAt(null);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void restoreFromSnapshot(JsonNode snapshot, RecoveryLifecycleState targetState) {
            String name = text(snapshot, "name");
            product.setName(StringUtils.hasText(name) ? name : "Restored product");
            product.setCategory(resolveCategory(text(snapshot, "categoryId")));
            product.setBrand(resolveBrand(text(snapshot, "brandId")));
            BigDecimal basePrice = decimal(snapshot, "basePrice");
            product.setBasePrice(basePrice == null ? BigDecimal.ZERO : basePrice);
            product.setAttributes(attributes(snapshot.get("attributes")));
            product.setStatus(status(snapshot, "status"));
            product.setAllowBackorder(bool(snapshot, "allowBackorder"));
            product.setFlashSale(bool(snapshot, "flashSale"));
            product.setTrending(bool(snapshot, "trending"));
            product.setBestSeller(bool(snapshot, "bestSeller"));
            product.setAverageRating(doubleValue(snapshot, "averageRating", 0D));
            product.setReviewCount(integer(snapshot, "reviewCount", 0));
            product.setPopularityScore(integer(snapshot, "popularityScore", 0));
            product.setShortDescription(text(snapshot, "shortDescription"));
            product.setLongDescription(text(snapshot, "longDescription"));
            product.setTargetAudience(text(snapshot, "targetAudience"));
            product.setBarcode(text(snapshot, "barcode"));
            product.setQrCode(text(snapshot, "qrCode"));
            product.setSeoTitle(text(snapshot, "seoTitle"));
            product.setSeoDescription(text(snapshot, "seoDescription"));
            product.setSeoSlug(text(snapshot, "seoSlug"));
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
         * Resolves category when present in snapshot payload.
         *
         * @param value The serialized category id.
         * @return The resolved category.
         */
        private Category resolveCategory(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            try {
                UUID categoryId = UUID.fromString(value.trim());
                return categoryRepository.findById(categoryId).orElse(null);
            } catch (RuntimeException exception) {
                return null;
            }
        }

        /**
         * Resolves brand when present in snapshot payload.
         *
         * @param value The serialized brand id.
         * @return The resolved brand.
         */
        private Brand resolveBrand(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            try {
                UUID brandId = UUID.fromString(value.trim());
                return brandRepository.findById(brandId).orElse(null);
            } catch (RuntimeException exception) {
                return null;
            }
        }

        /**
         * Reads a text field from a product snapshot.
         *
         * @param snapshot The product snapshot payload.
         * @param fieldName The field name.
         * @return The field value.
         */
        private String text(JsonNode snapshot, String fieldName) {
            JsonNode field = snapshot.get(fieldName);
            return field == null || field.isNull() ? null : field.asText();
        }

        /**
         * Reads a boolean field from a product snapshot.
         *
         * @param snapshot The product snapshot payload.
         * @param fieldName The field name.
         * @return The field value.
         */
        private boolean bool(JsonNode snapshot, String fieldName) {
            JsonNode field = snapshot.get(fieldName);
            return field != null && !field.isNull() && field.asBoolean();
        }

        /**
         * Reads a decimal field from a product snapshot.
         *
         * @param snapshot The product snapshot payload.
         * @param fieldName The field name.
         * @return The field value.
         */
        private BigDecimal decimal(JsonNode snapshot, String fieldName) {
            JsonNode field = snapshot.get(fieldName);
            return field == null || field.isNull() ? null : field.decimalValue();
        }

        /**
         * Reads an integer field from a product snapshot.
         *
         * @param snapshot The product snapshot payload.
         * @param fieldName The field name.
         * @param defaultValue The default fallback value.
         * @return The field value.
         */
        private int integer(JsonNode snapshot, String fieldName, int defaultValue) {
            JsonNode field = snapshot.get(fieldName);
            return field == null || field.isNull() ? defaultValue : field.asInt(defaultValue);
        }

        /**
         * Reads a double field from a product snapshot.
         *
         * @param snapshot The product snapshot payload.
         * @param fieldName The field name.
         * @param defaultValue The default fallback value.
         * @return The field value.
         */
        private double doubleValue(JsonNode snapshot, String fieldName, double defaultValue) {
            JsonNode field = snapshot.get(fieldName);
            return field == null || field.isNull() ? defaultValue : field.asDouble(defaultValue);
        }

        /**
         * Resolves product status from the snapshot payload.
         *
         * @param snapshot The product snapshot payload.
         * @param fieldName The field name.
         * @return The resolved product status.
         */
        private ProductStatus status(JsonNode snapshot, String fieldName) {
            String value = text(snapshot, fieldName);
            if (!StringUtils.hasText(value)) {
                return ProductStatus.DRAFT;
            }
            try {
                return ProductStatus.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                return ProductStatus.DRAFT;
            }
        }

        /**
         * Resolves product attributes from a JSON object snapshot.
         *
         * @param node The serialized attributes node.
         * @return The resolved attributes map.
         */
        private Map<String, Object> attributes(JsonNode node) {
            if (node == null || node.isNull() || !node.isObject()) {
                return new LinkedHashMap<>();
            }
            Map<String, Object> values = new LinkedHashMap<>();
            node.fields().forEachRemaining(entry -> values.put(entry.getKey(), jsonValue(entry.getValue())));
            return values;
        }

        /**
         * Converts a JsonNode value into a serializable object tree.
         *
         * @param node The source node.
         * @return The converted value.
         */
        private Object jsonValue(JsonNode node) {
            if (node == null || node.isNull()) {
                return null;
            }
            if (node.isObject()) {
                Map<String, Object> map = new LinkedHashMap<>();
                node.fields().forEachRemaining(entry -> map.put(entry.getKey(), jsonValue(entry.getValue())));
                return map;
            }
            if (node.isArray()) {
                List<Object> values = new ArrayList<>();
                node.forEach(item -> values.add(jsonValue(item)));
                return values;
            }
            if (node.isBoolean()) {
                return node.asBoolean();
            }
            if (node.isIntegralNumber()) {
                return node.asLong();
            }
            if (node.isFloatingPointNumber()) {
                return node.asDouble();
            }
            return node.asText();
        }
    }
}
