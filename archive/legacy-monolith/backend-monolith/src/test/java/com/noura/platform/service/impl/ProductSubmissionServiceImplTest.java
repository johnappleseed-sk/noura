package com.noura.platform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.domain.entity.MerchantContract;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductSubmissionRequest;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.StoreTenant;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.MerchantContractStatus;
import com.noura.platform.domain.enums.ProductStatus;
import com.noura.platform.domain.enums.ProductSubmissionStatus;
import com.noura.platform.domain.enums.StoreTenantStatus;
import com.noura.platform.dto.product.ProductDto;
import com.noura.platform.dto.product.ProductRequest;
import com.noura.platform.dto.submission.ProductSubmissionCreateRequest;
import com.noura.platform.dto.submission.ProductSubmissionDecisionRequest;
import com.noura.platform.repository.BrandRepository;
import com.noura.platform.repository.CategoryRepository;
import com.noura.platform.repository.ProductDedupeCandidateRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.ProductSubmissionRequestRepository;
import com.noura.platform.repository.ProductSubmissionReviewRepository;
import com.noura.platform.repository.ProductVariantRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.StoreTenantRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.repository.UserStoreAssignmentRepository;
import com.noura.platform.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductSubmissionServiceImplTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitThenApprove_createsMasterProductAndMarksApproved() {
        ProductSubmissionRequestRepository submissionRepository = mock(ProductSubmissionRequestRepository.class);
        ProductSubmissionReviewRepository reviewRepository = mock(ProductSubmissionReviewRepository.class);
        ProductDedupeCandidateRepository dedupeCandidateRepository = mock(ProductDedupeCandidateRepository.class);
        StoreRepository storeRepository = mock(StoreRepository.class);
        StoreTenantRepository storeTenantRepository = mock(StoreTenantRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        UserStoreAssignmentRepository userStoreAssignmentRepository = mock(UserStoreAssignmentRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductVariantRepository productVariantRepository = mock(ProductVariantRepository.class);
        BrandRepository brandRepository = mock(BrandRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        ProductService productService = mock(ProductService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        ProductSubmissionServiceImpl service = new ProductSubmissionServiceImpl(
                submissionRepository,
                reviewRepository,
                dedupeCandidateRepository,
                storeRepository,
                storeTenantRepository,
                userAccountRepository,
                userStoreAssignmentRepository,
                productRepository,
                productVariantRepository,
                brandRepository,
                categoryRepository,
                productService,
                objectMapper
        );

        String adminEmail = "admin@noura.local";
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                adminEmail,
                "n/a",
                Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));

        UserAccount adminUser = new UserAccount();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail(adminEmail);
        when(userAccountRepository.findByEmailIgnoreCase(adminEmail)).thenReturn(Optional.of(adminUser));

        UUID storeId = UUID.randomUUID();
        Store store = new Store();
        store.setId(storeId);
        store.setName("Partner Store");
        store.setActive(true);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        MerchantContract contract = new MerchantContract();
        contract.setId(UUID.randomUUID());
        contract.setStatus(MerchantContractStatus.APPROVED);
        contract.setStartDate(java.time.LocalDate.now().minusDays(1));
        contract.setEndDate(java.time.LocalDate.now().plusDays(30));

        StoreTenant tenant = new StoreTenant();
        tenant.setId(UUID.randomUUID());
        tenant.setStore(store);
        tenant.setContract(contract);
        tenant.setStatus(StoreTenantStatus.ACTIVE);
        when(storeTenantRepository.findByStoreId(storeId)).thenReturn(Optional.of(tenant));

        when(brandRepository.findByNameIgnoreCase(any())).thenReturn(Optional.empty());
        when(productRepository.findByBarcodeIgnoreCase(any())).thenReturn(Optional.empty());
        when(productRepository.findByDedupeFingerprint(any())).thenReturn(Optional.empty());
        when(productRepository.findTop20ByActiveTrueAndManufacturerPartNumberContainingIgnoreCaseOrderByUpdatedAtDesc(any())).thenReturn(List.of());
        when(productVariantRepository.findBySkuIn(any())).thenReturn(List.of());
        when(productRepository.findTop20ByActiveTrueAndNormalizedNameContainingIgnoreCaseOrderByUpdatedAtDesc(any())).thenReturn(List.of());

        when(submissionRepository.save(any(ProductSubmissionRequest.class))).thenAnswer(invocation -> {
            ProductSubmissionRequest saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ProductRequest product = new ProductRequest(
                "New Soap",
                null,
                null,
                "Household",
                "BrandX",
                new BigDecimal("2.50"),
                "BRC-123456",
                "MPN-1",
                Map.of(),
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(new com.noura.platform.dto.product.ProductInventoryRequest(storeId, 5, new BigDecimal("2.50")))
        );

        var submission = service.submit(storeId, new ProductSubmissionCreateRequest(product, "please approve"));
        assertThat(submission.status()).isEqualTo(ProductSubmissionStatus.PENDING_REVIEW);

        // Load pending submission for approve.
        ArgumentCaptor<ProductSubmissionRequest> savedSubmissionCaptor = ArgumentCaptor.forClass(ProductSubmissionRequest.class);
        verify(submissionRepository, atLeastOnce()).save(savedSubmissionCaptor.capture());
        UUID submissionId = submission.id();

        ProductSubmissionRequest stored = savedSubmissionCaptor.getAllValues().stream()
                .filter(item -> submissionId.equals(item.getId()))
                .findFirst()
                .orElseThrow();
        stored.setStatus(ProductSubmissionStatus.PENDING_REVIEW);
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(stored));
        when(reviewRepository.findBySubmissionIdOrderByOccurredAtDesc(submissionId)).thenReturn(List.of());
        when(dedupeCandidateRepository.findBySubmissionIdOrderByMatchScoreDesc(submissionId)).thenReturn(List.of());

        UUID masterId = UUID.randomUUID();
        ProductDto createdDto = new ProductDto(
                masterId,
                "New Soap",
                "Household",
                "BrandX",
                new BigDecimal("2.50"),
                false,
                false,
                false,
                0D,
                0,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                ProductStatus.DRAFT,
                true,
                false,
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                "BRC-123456",
                null
        );
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(createdDto);

        Product createdProduct = new Product();
        createdProduct.setId(masterId);
        createdProduct.setActive(true);
        createdProduct.setStatus(ProductStatus.DRAFT);
        when(productRepository.findById(masterId)).thenReturn(Optional.of(createdProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.approve(submissionId, new ProductSubmissionDecisionRequest("ok", null));

        assertThat(createdProduct.getStatus()).isEqualTo(ProductStatus.APPROVED);
        verify(productRepository).save(createdProduct);
    }
}
