package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.category.CategoryFilter;
import com.noura.platform.inventory.dto.category.CategoryRequest;
import com.noura.platform.inventory.dto.category.CategoryResponse;
import com.noura.platform.inventory.dto.category.CategoryTreeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Defines governed inventory category management operations.
 */
public interface CategoryService {

    /**
     * Creates a new inventory category.
     *
     * @param request The category request payload.
     * @return The created category response.
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Updates an existing inventory category.
     *
     * @param categoryId The inventory category identifier.
     * @param request The category request payload.
     * @return The updated category response.
     */
    CategoryResponse updateCategory(String categoryId, CategoryRequest request);

    /**
     * Retrieves a single inventory category.
     *
     * @param categoryId The inventory category identifier.
     * @return The resolved category response.
     */
    CategoryResponse getCategory(String categoryId);

    /**
     * Lists inventory categories for the supplied filter.
     *
     * @param filter The category filter.
     * @param pageable The pagination configuration.
     * @return The paginated category response.
     */
    Page<CategoryResponse> listCategories(CategoryFilter filter, Pageable pageable);

    /**
     * Lists the inventory category tree.
     *
     * @param activeOnly Whether inactive nodes should be excluded.
     * @return The hierarchical category tree.
     */
    List<CategoryTreeResponse> getCategoryTree(Boolean activeOnly);

    /**
     * Moves an inventory category into governed trash.
     *
     * @param categoryId The inventory category identifier.
     */
    void deleteCategory(String categoryId);
}
