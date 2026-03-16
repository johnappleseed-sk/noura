package com.noura.platform.inventory.mapper;

import com.noura.platform.inventory.domain.Category;
import com.noura.platform.inventory.dto.category.CategoryResponse;
import com.noura.platform.inventory.dto.category.CategorySummaryResponse;
import com.noura.platform.inventory.dto.category.CategoryTreeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    CategoryResponse toResponse(Category category);

    CategorySummaryResponse toSummary(Category category);

    default CategoryTreeResponse toTreeResponse(Category category, List<CategoryTreeResponse> children) {
        return new CategoryTreeResponse(
                category.getId(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getCategoryCode(),
                category.getName(),
                category.getLevel(),
                category.getSortOrder(),
                category.isActive(),
                children
        );
    }
}
