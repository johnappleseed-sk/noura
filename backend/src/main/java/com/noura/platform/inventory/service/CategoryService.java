package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.category.CategoryFilter;
import com.noura.platform.inventory.dto.category.CategoryRequest;
import com.noura.platform.inventory.dto.category.CategoryResponse;
import com.noura.platform.inventory.dto.category.CategoryTreeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(String categoryId, CategoryRequest request);

    CategoryResponse getCategory(String categoryId);

    Page<CategoryResponse> listCategories(CategoryFilter filter, Pageable pageable);

    List<CategoryTreeResponse> getCategoryTree(Boolean activeOnly);

    void deleteCategory(String categoryId);
}
