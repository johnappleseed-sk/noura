package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Brand;
import com.noura.platform.domain.entity.Category;
import com.noura.platform.domain.entity.Merchant;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductApprovalDecision;
import com.noura.platform.domain.entity.ProductSubmission;
import com.noura.platform.domain.entity.ProductVariant;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.StoreTenant;
import com.noura.platform.domain.entity.StoreProductReference;
import com.noura.platform.domain.enums.ApprovalDecisionType;
import com.noura.platform.domain.enums.MerchantStatus;
import com.noura.platform.domain.enums.ProductStatus;
import com.noura.platform.domain.enums.StoreStatus;
import com.noura.platform.domain.enums.SubmissionStatus;
import com.noura.platform.dto.superinventory.ApproveProductSubmissionRequest;
import com.noura.platform.dto.superinventory.CreateProductSubmissionRequest;
import com.noura.platform.dto.superinventory.ProductApprovalDecisionResponse;
import com.noura.platform.dto.superinventory.ProductSubmissionDetailResponse;
import com.noura.platform.dto.superinventory.ProductSubmissionResponse;
import com.noura.platform.dto.superinventory.RejectProductSubmissionRequest;
import com.noura.platform.dto.superinventory.StoreProductReferenceResponse;
import com.noura.platform.repository.BrandRepository;
import com.noura.platform.repository.CategoryRepository;
import com.noura.platform.repository.MerchantRepository;
import com.noura.platform.repository.ProductApprovalDecisionRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.ProductSubmissionRepository;
import com.noura.platform.repository.ProductVariantRepository;
import com.noura.platform.repository.StoreTenantRepository;
import com.noura.platform.repository.StoreProductReferenceRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.ProductDeduplicationService;
import com.noura.platform.service.SuperInventoryService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuperInventoryServiceImpl implements SuperInventoryService {

    private final ProductSubmissionRepository productSubmissionRepository;
    private final ProductApprovalDecisionRepository productApprovalDecisionRepository;
    private final StoreProductReferenceRepository storeProductReferenceRepository;
    private final MerchantRepository merchantRepository;
    private final StoreRepository storeRepository;
    private final StoreTenantRepository storeTenantRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductDeduplicationService productDeduplicationService;

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER','MANAGER','PRODUCT_MANAGER')")
    public ProductSubmissionResponse submitProductCandidate(CreateProductSubmissionRequest request) {
        Merchant merchant = requireActiveMerchant(request.merchantId());
        Store store = requireActiveStore(request.storeId());
        validateMerchantStoreRelationship(merchant, store);

        ProductSubmission submission = new ProductSubmission();
        submission.setMerchant(merchant);
        submission.setStore(store);
        submission.setProposedName(request.proposedName());
        submission.setProposedBrand(request.proposedBrand());
        submission.setProposedCategoryCode(request.proposedCategoryCode());
        submission.setProposedAttributesJson(safeAttributes(request.proposedAttributesJson()));
        submission.setProposedBarcode(trimToNull(request.proposedBarcode()));
        submission.setProposedSku(trimToNull(request.proposedSku()));
        submission.setSimilarityHash(productDeduplicationService.buildSimilarityHash(
                request.proposedName(),
                request.proposedBarcode(),
                request.proposedBrand()
        ));
        submission.setStatus(SubmissionStatus.PENDING_REVIEW);
        submission.setSubmittedAt(Instant.now());

        ProductSubmission saved = productSubmissionRepository.save(submission);
        return toResponse(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_PRODUCT_SUBMISSIONS_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public Page<ProductSubmissionResponse> listProductSubmissions(
            SubmissionStatus status,
            UUID merchantId,
            UUID storeId,
            String query,
            Pageable pageable
    ) {
        return productSubmissionRepository.findAll(buildListSpec(status, merchantId, storeId, query), pageable)
                .map(submission -> toResponse(submission, latestTargetProductId(submission.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_PRODUCT_SUBMISSIONS_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ProductSubmissionDetailResponse getProductSubmission(UUID submissionId) {
        ProductSubmission submission = requireSubmission(submissionId);
        List<ProductApprovalDecisionResponse> decisions = productApprovalDecisionRepository
                .findBySubmissionIdOrderByDecidedAtDesc(submissionId)
                .stream()
                .map(this::toDecisionResponse)
                .toList();
        return toDetailResponse(submission, decisions);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_PRODUCT_SUBMISSIONS_APPROVE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ProductSubmissionResponse approveProductSubmission(UUID submissionId, ApproveProductSubmissionRequest request) {
        ProductSubmission submission = requirePendingSubmission(submissionId);
        Product targetProduct = resolveApprovalTarget(submission, request);
        ensureStoreProductReference(submission.getStore(), targetProduct);

        String actor = currentActor();
        submission.setStatus(SubmissionStatus.APPROVED);
        submission.setReviewedAt(Instant.now());
        submission.setReviewedBy(actor);
        submission.setReviewNotes(trimToNull(request == null ? null : request.notes()));
        ProductSubmission saved = productSubmissionRepository.save(submission);

        ProductApprovalDecision decision = new ProductApprovalDecision();
        decision.setSubmission(saved);
        decision.setDecisionType(ApprovalDecisionType.APPROVED);
        decision.setTargetProduct(targetProduct);
        decision.setNotes(trimToNull(request == null ? null : request.notes()));
        decision.setDecidedAt(Instant.now());
        decision.setDecidedBy(actor);
        productApprovalDecisionRepository.save(decision);

        return toResponse(saved, targetProduct.getId());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_PRODUCT_SUBMISSIONS_APPROVE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ProductSubmissionResponse rejectProductSubmission(UUID submissionId, RejectProductSubmissionRequest request) {
        ProductSubmission submission = requirePendingSubmission(submissionId);
        String actor = currentActor();

        submission.setStatus(SubmissionStatus.REJECTED);
        submission.setReviewedAt(Instant.now());
        submission.setReviewedBy(actor);
        submission.setReviewNotes(trimToNull(request == null ? null : request.notes()));
        ProductSubmission saved = productSubmissionRepository.save(submission);

        ProductApprovalDecision decision = new ProductApprovalDecision();
        decision.setSubmission(saved);
        decision.setDecisionType(ApprovalDecisionType.REJECTED);
        decision.setNotes(trimToNull(request == null ? null : request.notes()));
        decision.setDecidedAt(Instant.now());
        decision.setDecidedBy(actor);
        productApprovalDecisionRepository.save(decision);

        return toResponse(saved, null);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_STORES_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public Page<StoreProductReferenceResponse> listStoreProductReferences(UUID storeId, UUID productId, Boolean active, Pageable pageable) {
        if (!storeRepository.existsById(storeId)) {
            throw new NotFoundException("STORE_NOT_FOUND", "Store not found");
        }

        if (productId != null && active != null) {
            return storeProductReferenceRepository
                    .findByStoreIdAndProductIdAndActive(storeId, productId, active, pageable)
                    .map(this::toReferenceResponse);
        }
        if (productId != null) {
            return storeProductReferenceRepository
                    .findByStoreIdAndProductId(storeId, productId, pageable)
                    .map(this::toReferenceResponse);
        }
        if (active != null) {
            return storeProductReferenceRepository
                    .findByStoreIdAndActive(storeId, active, pageable)
                    .map(this::toReferenceResponse);
        }
        return storeProductReferenceRepository
                .findByStoreId(storeId, pageable)
                .map(this::toReferenceResponse);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_STORES_UPDATE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public StoreProductReferenceResponse linkStoreProduct(UUID storeId, UUID productId) {
        Store store = requireActiveStore(storeId);
        Product product = requireLinkableProduct(productId);
        StoreProductReference reference = ensureStoreProductReference(store, product);
        return toReferenceResponse(reference);
    }

    private Product resolveApprovalTarget(ProductSubmission submission, ApproveProductSubmissionRequest request) {
        if (request != null && request.targetProductId() != null) {
            return requireLinkableProduct(request.targetProductId());
        }

        return productDeduplicationService.findPotentialMatch(submission)
                .filter(this::isLinkableProduct)
                .orElseGet(() -> createApprovedProduct(submission));
    }

    private Product createApprovedProduct(ProductSubmission submission) {
        Product product = new Product();
        product.setName(submission.getProposedName());
        product.setBrand(resolveBrand(submission.getProposedBrand()));
        product.setCategory(resolveCategory(submission.getProposedCategoryCode()));
        product.setBasePrice(BigDecimal.ZERO);
        product.setAttributes(safeAttributes(submission.getProposedAttributesJson()));
        product.setStatus(ProductStatus.APPROVED);
        product.setApprovalStatus("APPROVED");
        product.setActive(true);
        product.setBarcode(trimToNull(submission.getProposedBarcode()));
        product.setShortDescription(null);
        product.setLongDescription(null);
        product.setNormalizedName(normalizeName(submission.getProposedName()));
        product.setDedupeFingerprint(submission.getSimilarityHash());

        Product saved = productRepository.save(product);
        createPrimaryVariant(saved, submission);
        return saved;
    }

    private void createPrimaryVariant(Product product, ProductSubmission submission) {
        String sku = resolveVariantSku(submission);
        if (sku == null) {
            return;
        }
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setVariantName(product.getName());
        variant.setBarcode(trimToNull(submission.getProposedBarcode()));
        variant.setAttributes(safeAttributes(submission.getProposedAttributesJson()));
        variant.setStock(0);
        variant.setActive(true);
        productVariantRepository.save(variant);
    }

    private String resolveVariantSku(ProductSubmission submission) {
        String proposedSku = normalizeSku(submission.getProposedSku());
        if (proposedSku != null && !productVariantRepository.existsBySkuIgnoreCase(proposedSku)) {
            return proposedSku;
        }

        String proposedBarcode = trimToNull(submission.getProposedBarcode());
        if (proposedBarcode != null) {
            String barcodeSku = "SKU-" + proposedBarcode.replaceAll("[^A-Za-z0-9]+", "").toUpperCase(Locale.ROOT);
            if (!barcodeSku.equals("SKU-") && !productVariantRepository.existsBySkuIgnoreCase(barcodeSku)) {
                return barcodeSku;
            }
        }

        return "SKU-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private Brand resolveBrand(String proposedBrand) {
        String brandName = trimToNull(proposedBrand);
        if (brandName == null) {
            return null;
        }
        return brandRepository.findByNameIgnoreCase(brandName)
                .orElseGet(() -> {
                    Brand brand = new Brand();
                    brand.setName(brandName);
                    return brandRepository.save(brand);
                });
    }

    private Category resolveCategory(String proposedCategoryCode) {
        String categoryCode = trimToNull(proposedCategoryCode);
        if (categoryCode == null) {
            return null;
        }
        return categoryRepository.findByCodeIgnoreCase(categoryCode)
                .orElseThrow(() -> new BadRequestException("CATEGORY_NOT_FOUND", "Category code not found"));
    }

    private Product requireLinkableProduct(UUID productId) {
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        if (!isLinkableProduct(product)) {
            throw new BadRequestException("PRODUCT_NOT_APPROVED", "Product must be approved before linking");
        }
        return product;
    }

    private boolean isLinkableProduct(Product product) {
        return product.getStatus() == ProductStatus.APPROVED || product.getStatus() == ProductStatus.PUBLISHED;
    }

    private StoreProductReference ensureStoreProductReference(Store store, Product product) {
        return storeProductReferenceRepository.findByStoreIdAndProductId(store.getId(), product.getId())
                .map(existing -> {
                    existing.setActive(true);
                    return storeProductReferenceRepository.save(existing);
                })
                .orElseGet(() -> {
                    StoreProductReference created = new StoreProductReference();
                    created.setStore(store);
                    created.setProduct(product);
                    created.setActive(true);
                    return storeProductReferenceRepository.save(created);
                });
    }

    private ProductSubmission requireSubmission(UUID submissionId) {
        return productSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_SUBMISSION_NOT_FOUND", "Product submission not found"));
    }

    private ProductSubmission requirePendingSubmission(UUID submissionId) {
        ProductSubmission submission = requireSubmission(submissionId);
        if (submission.getStatus() != SubmissionStatus.PENDING_REVIEW) {
            throw new BadRequestException("SUBMISSION_ALREADY_REVIEWED", "Submission is not pending review");
        }
        return submission;
    }

    private Merchant requireActiveMerchant(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("MERCHANT_NOT_FOUND", "Merchant not found"));
        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new BadRequestException("MERCHANT_NOT_ACTIVE", "Merchant must be active");
        }
        return merchant;
    }

    private Store requireActiveStore(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new BadRequestException("STORE_NOT_ACTIVE", "Store must be active");
        }
        return store;
    }

    private void validateMerchantStoreRelationship(Merchant merchant, Store store) {
        if (store.getMerchantId() != null && store.getMerchantId().equals(merchant.getId())) {
            return;
        }

        StoreTenant tenant = storeTenantRepository.findByStoreId(store.getId()).orElse(null);
        if (tenant == null || tenant.getMerchant() == null) {
            throw new BadRequestException("STORE_MERCHANT_MISMATCH", "Store is not linked to the provided merchant");
        }
        if (!tenant.getMerchant().getId().equals(merchant.getId())) {
            throw new BadRequestException("STORE_MERCHANT_MISMATCH", "Store is not linked to the provided merchant");
        }
    }

    private Specification<ProductSubmission> buildListSpec(SubmissionStatus status, UUID merchantId, UUID storeId, String query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (merchantId != null) {
                predicates.add(cb.equal(root.get("merchant").get("id"), merchantId));
            }
            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("proposedName")), like),
                        cb.like(cb.lower(root.get("proposedBrand")), like),
                        cb.like(cb.lower(root.get("proposedCategoryCode")), like),
                        cb.like(cb.lower(root.get("proposedBarcode")), like),
                        cb.like(cb.lower(root.get("proposedSku")), like)
                ));
            }
            cq.orderBy(cb.desc(root.get("submittedAt")));
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private ProductSubmissionResponse toResponse(ProductSubmission submission, UUID targetProductId) {
        return new ProductSubmissionResponse(
                submission.getId(),
                submission.getMerchantId(),
                submission.getStoreId(),
                submission.getProposedName(),
                submission.getProposedBrand(),
                submission.getProposedCategoryCode(),
                safeAttributes(submission.getProposedAttributesJson()),
                submission.getProposedBarcode(),
                submission.getProposedSku(),
                submission.getSimilarityHash(),
                submission.getStatus(),
                submission.getSubmittedAt(),
                submission.getReviewedAt(),
                submission.getReviewedBy(),
                submission.getReviewNotes(),
                targetProductId
        );
    }

    private ProductSubmissionDetailResponse toDetailResponse(
            ProductSubmission submission,
            List<ProductApprovalDecisionResponse> decisions
    ) {
        UUID targetProductId = decisions.stream()
                .map(ProductApprovalDecisionResponse::targetProductId)
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);

        return new ProductSubmissionDetailResponse(
                submission.getId(),
                submission.getMerchantId(),
                submission.getStoreId(),
                submission.getProposedName(),
                submission.getProposedBrand(),
                submission.getProposedCategoryCode(),
                safeAttributes(submission.getProposedAttributesJson()),
                submission.getProposedBarcode(),
                submission.getProposedSku(),
                submission.getSimilarityHash(),
                submission.getStatus(),
                submission.getSubmittedAt(),
                submission.getReviewedAt(),
                submission.getReviewedBy(),
                submission.getReviewNotes(),
                targetProductId,
                decisions,
                submission.getCreatedAt(),
                submission.getUpdatedAt()
        );
    }

    private ProductApprovalDecisionResponse toDecisionResponse(ProductApprovalDecision decision) {
        return new ProductApprovalDecisionResponse(
                decision.getId(),
                decision.getSubmissionId(),
                decision.getDecisionType(),
                decision.getTargetProductId(),
                decision.getNotes(),
                decision.getDecidedAt(),
                decision.getDecidedBy()
        );
    }

    private StoreProductReferenceResponse toReferenceResponse(StoreProductReference reference) {
        return new StoreProductReferenceResponse(
                reference.getId(),
                reference.getStoreId(),
                reference.getProductId(),
                reference.isActive(),
                reference.getCreatedAt()
        );
    }

    private UUID latestTargetProductId(UUID submissionId) {
        return productApprovalDecisionRepository.findTopBySubmissionIdOrderByDecidedAtDesc(submissionId)
                .map(ProductApprovalDecision::getTargetProductId)
                .orElse(null);
    }

    private Map<String, Object> safeAttributes(Map<String, Object> attributes) {
        return new LinkedHashMap<>(attributes == null ? Map.of() : attributes);
    }

    private String currentActor() {
        return SecurityUtils.currentEmail();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeSku(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
