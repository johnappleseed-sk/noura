package com.noura.platform.inventory.service.impl;

import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.dto.recovery.RecoveryActionRequest;
import com.noura.platform.inventory.domain.Category;
import com.noura.platform.inventory.dto.category.CategoryRequest;
import com.noura.platform.inventory.dto.category.CategoryResponse;
import com.noura.platform.inventory.mapper.CategoryMapper;
import com.noura.platform.inventory.repository.InventoryCategoryRepository;
import com.noura.platform.inventory.service.CategoryService;
import com.noura.platform.service.recovery.RecoveryGovernanceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies governed recovery behavior for inventory categories.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private InventoryCategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private RecoveryGovernanceService recoveryGovernanceService;

    /**
     * Clears the security context after each test.
     */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Captures a governed recovery version when a category is created.
     */
    @Test
    void createCategoryShouldCaptureRecoveryVersion() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("architect@noura.test", "password"));
        CategoryService categoryService = new CategoryServiceImpl(categoryRepository, categoryMapper, recoveryGovernanceService);
        CategoryRequest request = new CategoryRequest(null, "APPAREL", "Apparel", "All apparel", 0, true);
        CategoryResponse response = new CategoryResponse(
                "cat-1",
                null,
                "APPAREL",
                "Apparel",
                "All apparel",
                0,
                0,
                true,
                Instant.parse("2026-03-11T00:00:00Z"),
                Instant.parse("2026-03-11T00:00:00Z")
        );

        when(categoryRepository.existsByCategoryCodeIgnoreCaseAndDeletedAtIsNull("APPAREL")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId("cat-1");
            return category;
        });
        when(categoryMapper.toResponse(any(Category.class))).thenReturn(response);

        CategoryResponse created = categoryService.createCategory(request);

        assertEquals("cat-1", created.id());
        verify(recoveryGovernanceService).captureVersion(
                eq("INVENTORY_CATEGORY"),
                eq("cat-1"),
                eq(RecoveryActionType.CREATE),
                eq("architect@noura.test"),
                eq("Inventory category created."),
                eq(Map.of("source", "inventory-category-service"))
        );
    }

    /**
     * Delegates category deletion to the governed trash action.
     */
    @Test
    void deleteCategoryShouldDelegateToGovernedTrash() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("architect@noura.test", "password"));
        CategoryService categoryService = new CategoryServiceImpl(categoryRepository, categoryMapper, recoveryGovernanceService);
        ArgumentCaptor<RecoveryActionRequest> requestCaptor = ArgumentCaptor.forClass(RecoveryActionRequest.class);

        categoryService.deleteCategory("cat-1");

        verify(recoveryGovernanceService).applyAction(requestCaptor.capture(), eq("architect@noura.test"));
        assertEquals("INVENTORY_CATEGORY", requestCaptor.getValue().entityType());
        assertEquals("cat-1", requestCaptor.getValue().entityId());
        assertEquals(RecoveryActionType.TRASH, requestCaptor.getValue().actionType());
    }
}
