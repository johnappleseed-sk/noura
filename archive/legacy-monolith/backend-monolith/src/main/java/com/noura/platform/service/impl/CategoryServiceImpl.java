package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Category;
import com.noura.platform.dto.category.CategoryResponse;
import com.noura.platform.dto.category.CreateCategoryRequest;
import com.noura.platform.repository.CategoryRepository;
import com.noura.platform.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PRODUCT_MANAGER')")
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String code = normalizeCode(request.code());
        String slug = normalizeSlug(request.slug());
        if (categoryRepository.existsByCodeIgnoreCase(code)) {
            throw new BadRequestException("CATEGORY_CODE_DUPLICATE", "Category code already exists");
        }
        if (categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw new BadRequestException("CATEGORY_SLUG_DUPLICATE", "Category slug already exists");
        }
        categoryRepository.findByNameIgnoreCase(request.name().trim()).ifPresent(existing -> {
            throw new BadRequestException("CATEGORY_NAME_DUPLICATE", "Category name already exists");
        });

        Category category = new Category();
        category.setCode(code);
        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setActive(request.active() == null || request.active());
        if (request.parentId() != null) {
            Category parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new NotFoundException("CATEGORY_PARENT_NOT_FOUND", "Parent category not found"));
            category.setParent(parent);
            category.setLevel(parent.getLevel() + 1);
        } else {
            category.setLevel(0);
        }

        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PRODUCT_MANAGER')")
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByLevelAscNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getParentId(),
                category.getCode(),
                category.getName(),
                category.getSlug(),
                category.getLevel(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("CATEGORY_CODE_REQUIRED", "Category code is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSlug(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("CATEGORY_SLUG_REQUIRED", "Category slug is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            throw new BadRequestException("CATEGORY_SLUG_REQUIRED", "Category slug is required");
        }
        return normalized;
    }
}
