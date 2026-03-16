package com.noura.platform.inventory.service.impl;

import com.noura.platform.common.exception.ApiException;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.dto.recovery.RecoveryActionRequest;
import com.noura.platform.inventory.domain.Category;
import com.noura.platform.inventory.dto.category.CategoryFilter;
import com.noura.platform.inventory.dto.category.CategoryRequest;
import com.noura.platform.inventory.dto.category.CategoryResponse;
import com.noura.platform.inventory.dto.category.CategoryTreeResponse;
import com.noura.platform.inventory.mapper.CategoryMapper;
import com.noura.platform.inventory.repository.InventoryCategoryRepository;
import com.noura.platform.inventory.service.CategoryService;
import com.noura.platform.service.recovery.RecoveryGovernanceService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Implements governed inventory category administration workflows.
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final InventoryCategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final RecoveryGovernanceService recoveryGovernanceService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public CategoryResponse createCategory(CategoryRequest request) {
        validateUniqueCode(request.categoryCode(), null);
        Category parent = resolveParent(request.parentId());
        Category category = new Category();
        applyCategory(category, request, parent);
        Category saved = categoryRepository.save(category);
        recoveryGovernanceService.captureVersion(
                "INVENTORY_CATEGORY",
                saved.getId(),
                RecoveryActionType.CREATE,
                resolveActor(),
                "Inventory category created.",
                Map.of("source", "inventory-category-service")
        );
        return categoryMapper.toResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public CategoryResponse updateCategory(String categoryId, CategoryRequest request) {
        Category category = getCategoryEntity(categoryId);
        validateUniqueCode(request.categoryCode(), categoryId);
        Category parent = resolveParent(request.parentId());
        validateNoCycle(category, parent);
        applyCategory(category, request, parent);
        Category saved = categoryRepository.save(category);
        refreshDescendantLevels(saved);
        recoveryGovernanceService.captureVersion(
                "INVENTORY_CATEGORY",
                saved.getId(),
                RecoveryActionType.UPDATE,
                resolveActor(),
                "Inventory category updated.",
                Map.of("source", "inventory-category-service")
        );
        return categoryMapper.toResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public CategoryResponse getCategory(String categoryId) {
        return categoryMapper.toResponse(getCategoryEntity(categoryId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public Page<CategoryResponse> listCategories(CategoryFilter filter, Pageable pageable) {
        CategoryFilter effectiveFilter = filter == null ? new CategoryFilter(null, null, null) : filter;
        return categoryRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.isNull(root.get("archivedAt")));
            if (StringUtils.hasText(effectiveFilter.query())) {
                String likeValue = "%" + effectiveFilter.query().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likeValue),
                        cb.like(cb.lower(root.get("categoryCode")), likeValue)
                ));
            }
            if (StringUtils.hasText(effectiveFilter.parentId())) {
                predicates.add(cb.equal(root.get("parent").get("id"), effectiveFilter.parentId()));
            }
            if (effectiveFilter.active() != null) {
                predicates.add(cb.equal(root.get("active"), effectiveFilter.active()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(categoryMapper::toResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree(Boolean activeOnly) {
        List<Category> categories = categoryRepository.findAllByDeletedAtIsNullOrderByLevelAscSortOrderAscNameAsc();
        Map<String, List<Category>> childrenByParent = new LinkedHashMap<>();
        for (Category category : categories) {
            if (category.getArchivedAt() != null) {
                continue;
            }
            if (Boolean.TRUE.equals(activeOnly) && !category.isActive()) {
                continue;
            }
            String parentId = category.getParent() != null ? category.getParent().getId() : "__ROOT__";
            childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(category);
        }
        return buildTree(childrenByParent, "__ROOT__");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public void deleteCategory(String categoryId) {
        recoveryGovernanceService.applyAction(
                new RecoveryActionRequest(
                        "INVENTORY_CATEGORY",
                        categoryId,
                        RecoveryActionType.TRASH,
                        "Inventory category moved to trash.",
                        null,
                        null,
                        null,
                        Map.of("source", "inventory-category-service")
                ),
                resolveActor()
        );
    }

    /**
     * Resolves a required inventory category aggregate that remains visible in the main workspace.
     *
     * @param categoryId The inventory category identifier.
     * @return The resolved inventory category aggregate.
     */
    private Category getCategoryEntity(String categoryId) {
        return categoryRepository.findByIdAndDeletedAtIsNull(categoryId)
                .filter(category -> category.getArchivedAt() == null)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));
    }

    /**
     * Resolves the optional parent inventory category for a request.
     *
     * @param parentId The optional parent category identifier.
     * @return The resolved parent category when present.
     */
    private Category resolveParent(String parentId) {
        if (!StringUtils.hasText(parentId)) {
            return null;
        }
        return categoryRepository.findByIdAndDeletedAtIsNull(parentId)
                .filter(category -> category.getArchivedAt() == null)
                .orElseThrow(() -> new NotFoundException("CATEGORY_PARENT_NOT_FOUND", "Parent category not found"));
    }

    /**
     * Validates uniqueness for the inventory category code.
     *
     * @param categoryCode The candidate category code.
     * @param currentCategoryId The current category identifier when updating.
     */
    private void validateUniqueCode(String categoryCode, String currentCategoryId) {
        boolean exists = currentCategoryId == null
                ? categoryRepository.existsByCategoryCodeIgnoreCaseAndDeletedAtIsNull(categoryCode)
                : categoryRepository.existsByCategoryCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(categoryCode, currentCategoryId);
        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_CODE_EXISTS", "Category code already exists");
        }
    }

    /**
     * Validates that a category-parent assignment does not create a cycle.
     *
     * @param category The candidate inventory category.
     * @param parent The candidate parent category.
     */
    private void validateNoCycle(Category category, Category parent) {
        if (category.getId() == null || parent == null) {
            return;
        }
        if (Objects.equals(category.getId(), parent.getId())) {
            throw new BadRequestException("CATEGORY_PARENT_INVALID", "Category cannot be its own parent");
        }
        Category cursor = parent;
        while (cursor != null) {
            if (Objects.equals(cursor.getId(), category.getId())) {
                throw new BadRequestException("CATEGORY_PARENT_INVALID", "Category parent would create a cycle");
            }
            cursor = Optional.ofNullable(cursor.getParent())
                    .map(Category::getId)
                    .flatMap(categoryRepository::findByIdAndDeletedAtIsNull)
                    .filter(item -> item.getArchivedAt() == null)
                    .orElse(null);
        }
    }

    /**
     * Applies a request payload to an inventory category aggregate.
     *
     * @param category The inventory category aggregate.
     * @param request The request payload.
     * @param parent The resolved parent category.
     */
    private void applyCategory(Category category, CategoryRequest request, Category parent) {
        category.setParent(parent);
        category.setCategoryCode(request.categoryCode().trim());
        category.setName(request.name().trim());
        category.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setActive(request.active() == null || request.active());
        category.setLevel(parent == null ? 0 : parent.getLevel() + 1);
        if (category.isActive()) {
            category.setDeletedAt(null);
            category.setArchivedAt(null);
        }
    }

    /**
     * Refreshes stored hierarchy levels for descendant categories.
     *
     * @param parent The parent category whose descendants should be recalculated.
     */
    private void refreshDescendantLevels(Category parent) {
        List<Category> children = categoryRepository.findAllByParent_IdAndDeletedAtIsNullOrderBySortOrderAscNameAsc(parent.getId());
        for (Category child : children) {
            if (child.getArchivedAt() != null) {
                continue;
            }
            int expectedLevel = parent.getLevel() + 1;
            if (child.getLevel() != expectedLevel) {
                child.setLevel(expectedLevel);
                categoryRepository.save(child);
            }
            refreshDescendantLevels(child);
        }
    }

    /**
     * Builds a nested inventory category tree response.
     *
     * @param childrenByParent The category children keyed by parent id.
     * @param parentId The current parent identifier.
     * @return The nested tree response.
     */
    private List<CategoryTreeResponse> buildTree(Map<String, List<Category>> childrenByParent, String parentId) {
        return childrenByParent.getOrDefault(parentId, List.of())
                .stream()
                .map(category -> categoryMapper.toTreeResponse(category, buildTree(childrenByParent, category.getId())))
                .toList();
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
