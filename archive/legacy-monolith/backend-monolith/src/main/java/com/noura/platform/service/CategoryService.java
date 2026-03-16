package com.noura.platform.service;

import com.noura.platform.dto.category.CategoryResponse;
import com.noura.platform.dto.category.CreateCategoryRequest;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CreateCategoryRequest request);

    List<CategoryResponse> listCategories();
}
