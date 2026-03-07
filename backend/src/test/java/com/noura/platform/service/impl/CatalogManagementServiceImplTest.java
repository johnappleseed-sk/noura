package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.domain.entity.Category;
import com.noura.platform.domain.entity.CategoryChangeRequest;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.CategoryChangeAction;
import com.noura.platform.domain.enums.CategoryChangeRequestStatus;
import com.noura.platform.dto.catalog.CategoryChangeReviewRequest;
import com.noura.platform.dto.catalog.CategoryRequest;
import com.noura.platform.dto.catalog.CategorySuggestionRequest;
import com.noura.platform.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogManagementServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private AttributeRepository attributeRepository;
    @Mock
    private AttributeSetRepository attributeSetRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private CategoryTranslationRepository categoryTranslationRepository;
    @Mock
    private ChannelCategoryMappingRepository channelCategoryMappingRepository;
    @Mock
    private CategoryChangeRequestRepository categoryChangeRequestRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductInventoryRepository productInventoryRepository;
    @Mock
    private ProductRepository productRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCategory_shouldRejectInvalidNameConvention() {
        CatalogManagementServiceImpl service = service();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createCategory(new CategoryRequest("electronics", null, null, null, null))
        );

        assertEquals("CATEGORY_NAME_INVALID", exception.getCode());
    }

    @Test
    void suggestCategory_shouldReturnBestMatchingCategoryFirst() {
        CatalogManagementServiceImpl service = service();
        Category smartphones = new Category();
        smartphones.setId(UUID.randomUUID());
        smartphones.setName("Smartphones");

        Category laptops = new Category();
        laptops.setId(UUID.randomUUID());
        laptops.setName("Laptops");

        when(categoryRepository.findAll()).thenReturn(List.of(laptops, smartphones));

        var result = service.suggestCategory(new CategorySuggestionRequest(
                "Top Smartphones for 2026",
                "Latest mobile devices and accessories",
                Map.of("brand", "Noura"),
                3,
                null
        ));

        assertFalse(result.suggestions().isEmpty());
        assertEquals(smartphones.getId(), result.suggestions().getFirst().categoryId());
    }

    @Test
    void approveCategoryChangeRequest_shouldApplyCreateAction() {
        CatalogManagementServiceImpl service = service();
        UserAccount requester = user("requester@noura.com");
        UserAccount reviewer = user("admin@noura.com");
        authenticateAs(reviewer.getEmail());

        UUID requestId = UUID.randomUUID();
        CategoryChangeRequest changeRequest = new CategoryChangeRequest();
        changeRequest.setId(requestId);
        changeRequest.setAction(CategoryChangeAction.CREATE);
        changeRequest.setStatus(CategoryChangeRequestStatus.PENDING);
        changeRequest.setRequestedBy(requester);
        changeRequest.setPayload(Map.of("name", "Electronics"));

        UUID createdCategoryId = UUID.randomUUID();
        when(categoryChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(changeRequest));
        when(userAccountRepository.findByEmailIgnoreCase(reviewer.getEmail())).thenReturn(Optional.of(reviewer));
        when(categoryRepository.findByNameIgnoreCase("Electronics")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(createdCategoryId);
            return saved;
        });
        when(categoryChangeRequestRepository.save(any(CategoryChangeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.approveCategoryChangeRequest(requestId, new CategoryChangeReviewRequest("approved"));

        assertEquals(CategoryChangeRequestStatus.APPROVED, result.status());
        assertEquals(createdCategoryId, result.categoryId());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void approveCategoryChangeRequest_shouldApplyRenameAction() {
        CatalogManagementServiceImpl service = service();
        UserAccount requester = user("requester@noura.com");
        UserAccount reviewer = user("admin@noura.com");
        authenticateAs(reviewer.getEmail());

        UUID requestId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Phones");
        category.setTaxonomyVersion(1);

        CategoryChangeRequest changeRequest = new CategoryChangeRequest();
        changeRequest.setId(requestId);
        changeRequest.setAction(CategoryChangeAction.RENAME);
        changeRequest.setStatus(CategoryChangeRequestStatus.PENDING);
        changeRequest.setCategory(category);
        changeRequest.setRequestedBy(requester);
        changeRequest.setPayload(Map.of("name", "Smartphones"));

        when(categoryChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(changeRequest));
        when(userAccountRepository.findByEmailIgnoreCase(reviewer.getEmail())).thenReturn(Optional.of(reviewer));
        when(categoryRepository.findByNameIgnoreCase("Smartphones")).thenReturn(Optional.empty());
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryChangeRequestRepository.save(any(CategoryChangeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.approveCategoryChangeRequest(requestId, new CategoryChangeReviewRequest("renamed"));

        assertEquals("Smartphones", category.getName());
        assertEquals(2, category.getTaxonomyVersion());
        assertEquals(CategoryChangeRequestStatus.APPROVED, result.status());
        assertEquals(categoryId, result.categoryId());
    }

    @Test
    void approveCategoryChangeRequest_shouldRejectDeleteWhenCategoryHasChildren() {
        CatalogManagementServiceImpl service = service();

        UUID requestId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Electronics");

        CategoryChangeRequest changeRequest = new CategoryChangeRequest();
        changeRequest.setId(requestId);
        changeRequest.setAction(CategoryChangeAction.DELETE);
        changeRequest.setStatus(CategoryChangeRequestStatus.PENDING);
        changeRequest.setCategory(category);
        changeRequest.setRequestedBy(user("requester@noura.com"));
        changeRequest.setPayload(Map.of());

        when(categoryChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(changeRequest));
        when(categoryRepository.existsByParentId(categoryId)).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.approveCategoryChangeRequest(requestId, new CategoryChangeReviewRequest("delete"))
        );

        assertEquals("CATEGORY_HAS_CHILDREN", ex.getCode());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(email, "n/a", List.of()));
    }

    private UserAccount user(String email) {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        return user;
    }

    private CatalogManagementServiceImpl service() {
        return new CatalogManagementServiceImpl(
                categoryRepository,
                attributeRepository,
                attributeSetRepository,
                userAccountRepository,
                categoryTranslationRepository,
                channelCategoryMappingRepository,
                categoryChangeRequestRepository,
                orderRepository,
                orderItemRepository,
                productInventoryRepository,
                productRepository
        );
    }
}
