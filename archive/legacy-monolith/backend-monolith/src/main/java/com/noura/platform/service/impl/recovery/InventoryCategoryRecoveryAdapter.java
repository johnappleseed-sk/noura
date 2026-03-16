package com.noura.platform.service.impl.recovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.enums.RecoveryLifecycleState;
import com.noura.platform.inventory.domain.Category;
import com.noura.platform.inventory.repository.InventoryCategoryRepository;
import com.noura.platform.inventory.repository.ProductCategoryRepository;
import com.noura.platform.service.recovery.RecoverableEntityAdapter;
import com.noura.platform.service.recovery.RecoverableEntityHandle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implements recovery governance behavior for inventory categories.
 */
@Component
@ConditionalOnProperty(prefix = "inventory", name = "enabled", havingValue = "true")
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${inventory.datasource.url:}')")
public class InventoryCategoryRecoveryAdapter implements RecoverableEntityAdapter {
    private static final String ENTITY_TYPE = "INVENTORY_CATEGORY";

    private final InventoryCategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;

    /**
     * Creates a new inventory category recovery adapter.
     *
     * @param categoryRepository The inventory category repository.
     * @param productCategoryRepository The product-category repository.
     */
    public InventoryCategoryRecoveryAdapter(
            InventoryCategoryRepository categoryRepository,
            ProductCategoryRepository productCategoryRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.productCategoryRepository = productCategoryRepository;
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
        return categoryRepository.findById(requireEntityId(entityId)).map(CategoryHandle::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecoverableEntityHandle restoreHandle(String entityId, JsonNode snapshot) {
        String normalizedEntityId = requireEntityId(entityId);
        Category category = categoryRepository.findById(normalizedEntityId).orElseGet(Category::new);
        category.setId(normalizedEntityId);
        CategoryHandle handle = new CategoryHandle(category);
        handle.restoreFromSnapshot(snapshot, RecoveryLifecycleState.ACTIVE);
        return handle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hardDelete(String entityId) {
        Category category = categoryRepository.findById(requireEntityId(entityId))
                .orElseThrow(() -> new NotFoundException("INVENTORY_CATEGORY_NOT_FOUND", "Inventory category not found."));
        ensureCanTrash(category.getId());
        categoryRepository.delete(category);
    }

    /**
     * Normalizes and validates a governed entity identifier.
     *
     * @param entityId The raw governed entity identifier.
     * @return The normalized identifier.
     */
    private String requireEntityId(String entityId) {
        if (!StringUtils.hasText(entityId)) {
            throw new BadRequestException("RECOVERY_ENTITY_ID_INVALID", "Inventory category identifier is required.");
        }
        return entityId.trim();
    }

    /**
     * Validates that an inventory category can be moved to trash or purged.
     *
     * @param categoryId The inventory category identifier.
     */
    private void ensureCanTrash(String categoryId) {
        if (categoryRepository.existsByParent_IdAndDeletedAtIsNull(categoryId)) {
            throw new BadRequestException(
                    "CATEGORY_HAS_CHILDREN",
                    "Cannot move an inventory category to trash while child categories still exist."
            );
        }
        if (productCategoryRepository.existsByCategory_IdAndProduct_DeletedAtIsNull(categoryId)) {
            throw new BadRequestException(
                    "CATEGORY_IN_USE",
                    "Cannot move an inventory category to trash while active products are still assigned."
            );
        }
    }

    /**
     * Provides a recoverable handle over an inventory category aggregate.
     */
    private final class CategoryHandle implements RecoverableEntityHandle {
        private final Category category;

        /**
         * Creates a new inventory category handle.
         *
         * @param category The inventory category aggregate.
         */
        private CategoryHandle(Category category) {
            this.category = category;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getEntityId() {
            return category.getId();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return StringUtils.hasText(category.getName()) ? category.getName() : category.getCategoryCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object toSnapshot() {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", category.getId());
            snapshot.put("parentId", category.getParent() == null ? null : category.getParent().getId());
            snapshot.put("categoryCode", category.getCategoryCode());
            snapshot.put("name", category.getName());
            snapshot.put("description", category.getDescription());
            snapshot.put("level", category.getLevel());
            snapshot.put("sortOrder", category.getSortOrder());
            snapshot.put("active", category.isActive());
            snapshot.put("deletedAt", category.getDeletedAt());
            snapshot.put("archivedAt", category.getArchivedAt());
            return snapshot;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void applyLifecycleState(RecoveryLifecycleState state) {
            switch (state) {
                case ACTIVE -> {
                    category.setActive(true);
                    category.setArchivedAt(null);
                    category.setDeletedAt(null);
                }
                case INACTIVE -> {
                    category.setActive(false);
                    category.setArchivedAt(null);
                    category.setDeletedAt(null);
                }
                case ARCHIVED -> {
                    category.setActive(false);
                    category.setArchivedAt(Instant.now());
                    category.setDeletedAt(null);
                }
                case TRASHED -> {
                    ensureCanTrash(category.getId());
                    category.setActive(false);
                    category.setArchivedAt(null);
                    category.setDeletedAt(Instant.now());
                }
                case PURGED, ANONYMIZED -> {
                    category.setActive(false);
                    category.setArchivedAt(category.getArchivedAt() == null ? Instant.now() : category.getArchivedAt());
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void restoreFromSnapshot(JsonNode snapshot, RecoveryLifecycleState targetState) {
            category.setParent(resolveParent(snapshot));
            category.setCategoryCode(text(snapshot, "categoryCode"));
            category.setName(text(snapshot, "name"));
            category.setDescription(text(snapshot, "description"));
            category.setSortOrder(integer(snapshot, "sortOrder", 0));
            category.setLevel(resolveLevel(category.getParent(), snapshot));
            applyLifecycleState(targetState);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void persist() {
            category.setLevel(category.getParent() == null ? 0 : category.getParent().getLevel() + 1);
            categoryRepository.save(category);
        }

        /**
         * Resolves the current parent category from a snapshot.
         *
         * @param snapshot The serialized snapshot payload.
         * @return The resolved parent category.
         */
        private Category resolveParent(JsonNode snapshot) {
            String parentId = text(snapshot, "parentId");
            if (!StringUtils.hasText(parentId)) {
                return null;
            }
            return categoryRepository.findById(parentId)
                    .orElseThrow(() -> new NotFoundException("CATEGORY_PARENT_NOT_FOUND", "Inventory category parent not found for restore."));
        }

        /**
         * Resolves the effective hierarchy level for the restored category.
         *
         * @param parent The resolved parent category.
         * @param snapshot The serialized snapshot payload.
         * @return The effective hierarchy level.
         */
        private int resolveLevel(Category parent, JsonNode snapshot) {
            if (parent != null) {
                return parent.getLevel() + 1;
            }
            return integer(snapshot, "level", 0);
        }

        /**
         * Reads a text field from an inventory category snapshot.
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
         * Reads an integer field from an inventory category snapshot.
         *
         * @param snapshot The serialized snapshot payload.
         * @param fieldName The requested field name.
         * @param fallback The fallback value.
         * @return The resolved integer value.
         */
        private int integer(JsonNode snapshot, String fieldName, int fallback) {
            JsonNode field = snapshot.get(fieldName);
            return field == null || field.isNull() ? fallback : field.asInt();
        }
    }
}
