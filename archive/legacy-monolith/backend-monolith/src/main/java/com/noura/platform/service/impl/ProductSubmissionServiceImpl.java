package com.noura.platform.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.ForbiddenException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Brand;
import com.noura.platform.domain.entity.Category;
import com.noura.platform.domain.entity.Merchant;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductDedupeCandidate;
import com.noura.platform.domain.entity.ProductSubmissionRequest;
import com.noura.platform.domain.entity.ProductSubmissionReview;
import com.noura.platform.domain.entity.ProductVariant;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.StoreTenant;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.entity.UserStoreAssignment;
import com.noura.platform.domain.enums.MerchantContractStatus;
import com.noura.platform.domain.enums.ProductStatus;
import com.noura.platform.domain.enums.ProductSubmissionReviewAction;
import com.noura.platform.domain.enums.ProductSubmissionStatus;
import com.noura.platform.domain.enums.StoreTenantStatus;
import com.noura.platform.dto.product.ProductInventoryRequest;
import com.noura.platform.dto.product.ProductRequest;
import com.noura.platform.dto.submission.*;
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
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.ProductService;
import com.noura.platform.service.ProductSubmissionService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductSubmissionServiceImpl implements ProductSubmissionService {

    private static final int MAX_DEDUPE_CANDIDATES = 10;

    private final ProductSubmissionRequestRepository submissionRepository;
    private final ProductSubmissionReviewRepository reviewRepository;
    private final ProductDedupeCandidateRepository dedupeCandidateRepository;
    private final StoreRepository storeRepository;
    private final StoreTenantRepository storeTenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserStoreAssignmentRepository userStoreAssignmentRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER','MANAGER','PRODUCT_MANAGER')")
    public ProductSubmissionDto submit(UUID storeId, ProductSubmissionCreateRequest request) {
        Store store = requireStore(storeId);
        requireStoreAccess(storeId);
        requireStoreContractValid(storeId);
        ProductRequest product = requireProductPayload(request);
        validateStoreScopedPayload(storeId, product);

        UserAccount requester = currentUser();
        StoreTenant tenant = storeTenantRepository.findByStoreId(storeId).orElse(null);
        Merchant merchant = tenant == null ? null : tenant.getMerchant();

        ProductSubmissionRequest submission = new ProductSubmissionRequest();
        submission.setStore(store);
        submission.setMerchant(merchant);
        submission.setParentSubmission(null);
        submission.setRevisionNumber(1);
        submission.setStatus(ProductSubmissionStatus.PENDING_REVIEW);
        submission.setRequestedBy(requester);

        Map<String, Object> payload = asMap(request);
        submission.setPayload(payload);

        String normalizedName = normalizeProductName(product.name());
        submission.setNormalizedName(normalizedName);
        submission.setBarcode(trimToNull(product.barcode()));
        submission.setManufacturerPartNumber(trimToNull(product.manufacturerPartNumber()));
        submission.setDedupeFingerprint(buildDedupeFingerprint(product, normalizedName));

        ProductSubmissionRequest saved = submissionRepository.save(submission);
        writeReview(saved, ProductSubmissionReviewAction.SUBMITTED, null, null);
        computeAndPersistDedupeCandidates(saved, product);
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER','MANAGER','PRODUCT_MANAGER')")
    public ProductSubmissionDto resubmit(UUID storeId, UUID submissionId, ProductSubmissionCreateRequest request) {
        Store store = requireStore(storeId);
        requireStoreAccess(storeId);
        requireStoreContractValid(storeId);

        ProductSubmissionRequest previous = submissionRepository.findByIdAndStoreId(submissionId, storeId)
                .orElseThrow(() -> new NotFoundException("SUBMISSION_NOT_FOUND", "Submission not found"));
        if (previous.getStatus() != ProductSubmissionStatus.REVISION_REQUESTED) {
            throw new BadRequestException("SUBMISSION_NOT_REVISION", "Submission is not in REVISION_REQUESTED state");
        }

        ProductRequest product = requireProductPayload(request);
        validateStoreScopedPayload(storeId, product);

        UserAccount requester = currentUser();
        StoreTenant tenant = storeTenantRepository.findByStoreId(storeId).orElse(null);
        Merchant merchant = tenant == null ? null : tenant.getMerchant();

        ProductSubmissionRequest next = new ProductSubmissionRequest();
        next.setStore(store);
        next.setMerchant(merchant);
        next.setParentSubmission(previous);
        next.setRevisionNumber(previous.getRevisionNumber() + 1);
        next.setStatus(ProductSubmissionStatus.PENDING_REVIEW);
        next.setRequestedBy(requester);
        next.setPayload(asMap(request));

        String normalizedName = normalizeProductName(product.name());
        next.setNormalizedName(normalizedName);
        next.setBarcode(trimToNull(product.barcode()));
        next.setManufacturerPartNumber(trimToNull(product.manufacturerPartNumber()));
        next.setDedupeFingerprint(buildDedupeFingerprint(product, normalizedName));

        ProductSubmissionRequest saved = submissionRepository.save(next);
        writeReview(saved, ProductSubmissionReviewAction.RESUBMITTED, null, null);
        computeAndPersistDedupeCandidates(saved, product);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER','MANAGER','PRODUCT_MANAGER')")
    public Page<ProductSubmissionDto> listForStore(UUID storeId, ProductSubmissionStatus status, Pageable pageable) {
        requireStoreAccess(storeId);
        requireStoreContractValid(storeId);
        Specification<ProductSubmissionRequest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("store").get("id"), storeId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return submissionRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_PRODUCT_SUBMISSIONS_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public Page<ProductSubmissionDto> listForAdmin(ProductSubmissionStatus status, String query, Boolean duplicatesOnly, Pageable pageable) {
        Specification<ProductSubmissionRequest> spec = buildAdminListSpec(status, query, duplicatesOnly);
        return submissionRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER','MANAGER','PRODUCT_MANAGER')")
    public ProductSubmissionDetailDto getForStore(UUID storeId, UUID submissionId) {
        requireStoreAccess(storeId);
        requireStoreContractValid(storeId);
        ProductSubmissionRequest submission = submissionRepository.findByIdAndStoreId(submissionId, storeId)
                .orElseThrow(() -> new NotFoundException("SUBMISSION_NOT_FOUND", "Submission not found"));
        return toDetailDto(submission);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_PRODUCT_SUBMISSIONS_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ProductSubmissionDetailDto getForAdmin(UUID submissionId) {
        ProductSubmissionRequest submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("SUBMISSION_NOT_FOUND", "Submission not found"));
        return toDetailDto(submission);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_PRODUCT_SUBMISSIONS_APPROVE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ProductSubmissionDto approve(UUID submissionId, ProductSubmissionDecisionRequest request) {
        ProductSubmissionRequest submission = requirePending(submissionId);
        ProductSubmissionDetailDto detail = toDetailDto(submission);
        ProductRequest productRequest = detail.product();

        if (request != null && request.existingMasterProductId() != null) {
            return linkDuplicate(submissionId, request);
        }

        Product created = createMasterProductFromSubmission(productRequest);
        created.setStatus(ProductStatus.APPROVED);
        productRepository.save(created);

        submission.setMatchedMasterProduct(created);
        submission.setStatus(ProductSubmissionStatus.APPROVED);
        submission.setReviewedBy(currentUser());
        submission.setReviewedAt(Instant.now());
        submission.setReviewNote(trimToNull(request == null ? null : request.note()));
        ProductSubmissionRequest saved = submissionRepository.save(submission);
        writeReview(saved, ProductSubmissionReviewAction.APPROVED, request == null ? null : request.note(), created);
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_PRODUCT_SUBMISSIONS_APPROVE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ProductSubmissionDto reject(UUID submissionId, ProductSubmissionDecisionRequest request) {
        ProductSubmissionRequest submission = requirePending(submissionId);
        submission.setStatus(ProductSubmissionStatus.REJECTED);
        submission.setReviewedBy(currentUser());
        submission.setReviewedAt(Instant.now());
        submission.setReviewNote(trimToNull(request == null ? null : request.note()));
        ProductSubmissionRequest saved = submissionRepository.save(submission);
        writeReview(saved, ProductSubmissionReviewAction.REJECTED, request == null ? null : request.note(), null);
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_PRODUCT_SUBMISSIONS_APPROVE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ProductSubmissionDto requestRevision(UUID submissionId, ProductSubmissionDecisionRequest request) {
        ProductSubmissionRequest submission = requirePending(submissionId);
        submission.setStatus(ProductSubmissionStatus.REVISION_REQUESTED);
        submission.setReviewedBy(currentUser());
        submission.setReviewedAt(Instant.now());
        submission.setReviewNote(trimToNull(request == null ? null : request.note()));
        ProductSubmissionRequest saved = submissionRepository.save(submission);
        writeReview(saved, ProductSubmissionReviewAction.REVISION_REQUESTED, request == null ? null : request.note(), null);
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('PERM_PRODUCT_SUBMISSIONS_APPROVE') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ProductSubmissionDto linkDuplicate(UUID submissionId, ProductSubmissionDecisionRequest request) {
        ProductSubmissionRequest submission = requirePending(submissionId);
        if (request == null || request.existingMasterProductId() == null) {
            throw new BadRequestException("MASTER_PRODUCT_REQUIRED", "existingMasterProductId is required to link a duplicate");
        }
        Product master = productRepository.findByIdAndActiveTrue(request.existingMasterProductId())
                .orElseThrow(() -> new NotFoundException("MASTER_PRODUCT_NOT_FOUND", "Master product not found"));

        ProductSubmissionDetailDto detail = toDetailDto(submission);
        ProductRequest productRequest = detail.product();
        ensureStoreInventoryMapping(submission.getStore(), master, productRequest);

        submission.setMatchedMasterProduct(master);
        submission.setStatus(ProductSubmissionStatus.APPROVED);
        submission.setReviewedBy(currentUser());
        submission.setReviewedAt(Instant.now());
        submission.setReviewNote(trimToNull(request.note()));
        ProductSubmissionRequest saved = submissionRepository.save(submission);
        writeReview(saved, ProductSubmissionReviewAction.DUPLICATE_LINKED, request.note(), master);
        return toDto(saved);
    }

    private ProductSubmissionRequest requirePending(UUID submissionId) {
        ProductSubmissionRequest submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("SUBMISSION_NOT_FOUND", "Submission not found"));
        if (submission.getStatus() != ProductSubmissionStatus.PENDING_REVIEW) {
            throw new BadRequestException("SUBMISSION_FINALIZED", "Submission is not in PENDING_REVIEW state");
        }
        return submission;
    }

    private Product createMasterProductFromSubmission(ProductRequest productRequest) {
        try {
            // ProductService enforces platform-side validation and will create the master Product + variants/media/inventory.
            var dto = productService.createProduct(productRequest);
            return productRepository.findById(dto.id())
                    .orElseThrow(() -> new NotFoundException("MASTER_PRODUCT_NOT_FOUND", "Created master product not found"));
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("SUBMISSION_APPROVE_FAILED", "Failed to create master product from submission");
        }
    }

    private void ensureStoreInventoryMapping(Store store, Product master, ProductRequest productRequest) {
        if (store == null) {
            return;
        }
        ProductInventoryRequest inventory = extractInventoryForStore(store.getId(), productRequest);
        // Reuse existing ProductService inventory upsert semantics by calling repository directly is fine:
        // product_inventory unique (product_id, store_id) ensures idempotency.
        var existing = productService.upsertInventory(master.getId(), new ProductInventoryRequest(
                store.getId(),
                inventory.stock(),
                inventory.storePrice()
        ));
        // Keep published/visible defaults on existing mapping. Store-specific overrides can be adjusted via dedicated APIs.
    }

    private ProductInventoryRequest extractInventoryForStore(UUID storeId, ProductRequest productRequest) {
        if (productRequest.inventory() == null || productRequest.inventory().isEmpty()) {
            BigDecimal price = productRequest.price();
            if (price == null && productRequest.variants() != null) {
                price = productRequest.variants().stream().map(v -> v.price()).filter(p -> p != null).findFirst().orElse(BigDecimal.ZERO);
            }
            return new ProductInventoryRequest(storeId, 0, price == null ? BigDecimal.ZERO : price);
        }
        return productRequest.inventory().stream()
                .filter(item -> item != null && storeId.equals(item.storeId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("STORE_INVENTORY_REQUIRED", "Submission payload must include inventory for the submitting store"));
    }

    private void computeAndPersistDedupeCandidates(ProductSubmissionRequest submission, ProductRequest product) {
        dedupeCandidateRepository.deleteBySubmissionId(submission.getId());
        Map<UUID, CandidateAccumulator> candidates = new LinkedHashMap<>();

        String barcode = trimToNull(product.barcode());
        if (barcode != null) {
            productRepository.findByBarcodeIgnoreCase(barcode).ifPresent(found -> addCandidate(candidates, found, new BigDecimal("1.0000"), "BARCODE_MATCH", Map.of("barcode", barcode)));
        }

        String fingerprint = buildDedupeFingerprint(product, submission.getNormalizedName());
        if (fingerprint != null && !fingerprint.isBlank()) {
            productRepository.findByDedupeFingerprint(fingerprint).ifPresent(found -> addCandidate(candidates, found, new BigDecimal("0.9000"), "FINGERPRINT_MATCH", Map.of("fingerprint", fingerprint)));
        }

        String mpn = trimToNull(product.manufacturerPartNumber());
        if (mpn != null) {
            productRepository.findTop20ByActiveTrueAndManufacturerPartNumberContainingIgnoreCaseOrderByUpdatedAtDesc(mpn)
                    .forEach(found -> addCandidate(candidates, found, new BigDecimal("0.8000"), "MPN_MATCH", Map.of("mpn", mpn)));
        }

        List<String> skus = product.variants() == null ? List.of() : product.variants().stream()
                .map(v -> v == null ? null : trimToNull(v.sku()))
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        if (!skus.isEmpty()) {
            productVariantRepository.findBySkuIn(skus).stream()
                    .map(ProductVariant::getProduct)
                    .distinct()
                    .forEach(found -> addCandidate(candidates, found, new BigDecimal("1.0000"), "SKU_MATCH", Map.of("skus", skus)));
        }

        if (submission.getNormalizedName() != null && !submission.getNormalizedName().isBlank()) {
            productRepository.findTop20ByActiveTrueAndNormalizedNameContainingIgnoreCaseOrderByUpdatedAtDesc(submission.getNormalizedName())
                    .forEach(found -> addCandidate(candidates, found, new BigDecimal("0.5000"), "NAME_MATCH", Map.of("normalizedName", submission.getNormalizedName())));
        }

        List<CandidateAccumulator> ordered = candidates.values().stream()
                .sorted(Comparator.comparing((CandidateAccumulator c) -> c.score).reversed())
                .limit(MAX_DEDUPE_CANDIDATES)
                .toList();

        boolean potentialDuplicate = !ordered.isEmpty();
        submission.setPotentialDuplicate(potentialDuplicate);
        submissionRepository.save(submission);

        for (CandidateAccumulator candidate : ordered) {
            ProductDedupeCandidate row = new ProductDedupeCandidate();
            row.setSubmission(submission);
            row.setMasterProduct(candidate.product);
            row.setMatchScore(candidate.score);
            row.setMatchReason(candidate.reason);
            row.setDetail(candidate.detail);
            dedupeCandidateRepository.save(row);
        }
    }

    private void addCandidate(Map<UUID, CandidateAccumulator> candidates,
                              Product product,
                              BigDecimal score,
                              String reason,
                              Map<String, Object> detail) {
        if (product == null || product.getId() == null) {
            return;
        }
        CandidateAccumulator existing = candidates.get(product.getId());
        if (existing == null || existing.score.compareTo(score) < 0) {
            candidates.put(product.getId(), new CandidateAccumulator(product, score, reason, new LinkedHashMap<>(detail == null ? Map.of() : detail)));
        }
    }

    private ProductSubmissionDetailDto toDetailDto(ProductSubmissionRequest submission) {
        Map<String, Object> payload = submission.getPayload() == null ? Map.of() : submission.getPayload();
        ProductSubmissionCreateRequest createRequest = fromMap(payload, ProductSubmissionCreateRequest.class);
        ProductRequest product = createRequest == null ? null : createRequest.product();
        String submitNote = createRequest == null ? null : createRequest.note();

        List<ProductDedupeCandidateDto> candidates = dedupeCandidateRepository.findBySubmissionIdOrderByMatchScoreDesc(submission.getId()).stream()
                .map(this::toDto)
                .toList();
        List<ProductSubmissionReviewDto> reviews = reviewRepository.findBySubmissionIdOrderByOccurredAtDesc(submission.getId()).stream()
                .map(this::toDto)
                .toList();

        return new ProductSubmissionDetailDto(
                submission.getId(),
                submission.getStore().getId(),
                submission.getStore().getName(),
                submission.getMerchant() == null ? null : submission.getMerchant().getId(),
                submission.getMerchant() == null ? null : submission.getMerchant().getName(),
                submission.getParentSubmission() == null ? null : submission.getParentSubmission().getId(),
                submission.getRevisionNumber(),
                submission.getStatus(),
                product,
                submitNote,
                submission.isPotentialDuplicate(),
                submission.getMatchedMasterProduct() == null ? null : submission.getMatchedMasterProduct().getId(),
                submission.getRequestedBy() == null ? null : submission.getRequestedBy().getEmail(),
                submission.getReviewedBy() == null ? null : submission.getReviewedBy().getEmail(),
                submission.getReviewedAt(),
                submission.getReviewNote(),
                candidates,
                reviews,
                submission.getCreatedAt()
        );
    }

    private ProductSubmissionDto toDto(ProductSubmissionRequest submission) {
        return new ProductSubmissionDto(
                submission.getId(),
                submission.getStore().getId(),
                submission.getStore().getName(),
                submission.getMerchant() == null ? null : submission.getMerchant().getId(),
                submission.getMerchant() == null ? null : submission.getMerchant().getName(),
                submission.getRevisionNumber(),
                submission.getStatus(),
                submission.isPotentialDuplicate(),
                submission.getMatchedMasterProduct() == null ? null : submission.getMatchedMasterProduct().getId(),
                submission.getRequestedBy() == null ? null : submission.getRequestedBy().getEmail(),
                submission.getCreatedAt(),
                submission.getReviewedAt()
        );
    }

    private ProductDedupeCandidateDto toDto(ProductDedupeCandidate candidate) {
        Product product = candidate.getMasterProduct();
        return new ProductDedupeCandidateDto(
                candidate.getId(),
                product.getId(),
                product.getName(),
                product.getBarcode(),
                candidate.getMatchScore(),
                candidate.getMatchReason(),
                candidate.getDetail()
        );
    }

    private ProductSubmissionReviewDto toDto(ProductSubmissionReview review) {
        return new ProductSubmissionReviewDto(
                review.getId(),
                review.getAction(),
                review.getReviewerEmail(),
                review.getNote(),
                review.getMasterProduct() == null ? null : review.getMasterProduct().getId(),
                review.getOccurredAt()
        );
    }

    private Specification<ProductSubmissionRequest> buildAdminListSpec(ProductSubmissionStatus status, String query, Boolean duplicatesOnly) {
        return (root, q, cb) -> {
            root.fetch("store", JoinType.LEFT);
            root.fetch("merchant", JoinType.LEFT);
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (Boolean.TRUE.equals(duplicatesOnly)) {
                predicates.add(cb.isTrue(root.get("potentialDuplicate")));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("normalizedName")), like),
                        cb.like(cb.lower(root.get("manufacturerPartNumber")), like),
                        cb.like(cb.lower(root.get("barcode")), like),
                        cb.like(cb.lower(root.get("store").get("name")), like),
                        cb.like(cb.lower(root.get("merchant").get("name")), like)
                ));
            }
            q.orderBy(cb.desc(root.get("createdAt")));
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void writeReview(ProductSubmissionRequest submission, ProductSubmissionReviewAction action, String note, Product masterProduct) {
        ProductSubmissionReview review = new ProductSubmissionReview();
        review.setSubmission(submission);
        review.setAction(action);
        review.setReviewer(currentUserOptional());
        review.setReviewerEmail(SecurityUtils.currentEmail());
        review.setNote(trimToNull(note));
        review.setMasterProduct(masterProduct);
        review.setOccurredAt(Instant.now());
        reviewRepository.save(review);
    }

    private Store requireStore(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
    }

    private void requireStoreAccess(UUID storeId) {
        if (isPlatformAdmin()) {
            return;
        }
        UserAccount user = currentUser();
        UserStoreAssignment assignment = userStoreAssignmentRepository.findByUserIdAndStoreIdAndActiveTrue(user.getId(), storeId)
                .orElse(null);
        if (assignment == null) {
            throw new ForbiddenException("STORE_ACCESS_DENIED", "You are not assigned to this store");
        }
    }

    private void requireStoreContractValid(UUID storeId) {
        Store store = requireStore(storeId);
        if (!store.isActive()) {
            throw new ForbiddenException("STORE_INACTIVE", "Store is inactive");
        }
        StoreTenant tenant = storeTenantRepository.findByStoreId(storeId).orElse(null);
        if (tenant == null) {
            return;
        }
        if (tenant.getStatus() != StoreTenantStatus.ACTIVE) {
            throw new ForbiddenException("STORE_TENANT_INACTIVE", "Store tenant is not active");
        }
        if (tenant.getContract() == null || tenant.getContract().getStatus() != MerchantContractStatus.APPROVED) {
            throw new ForbiddenException("STORE_CONTRACT_NOT_APPROVED", "Store contract is not approved");
        }
        LocalDate today = LocalDate.now();
        LocalDate start = tenant.getContract().getStartDate();
        LocalDate end = tenant.getContract().getEndDate();
        if (start != null && today.isBefore(start)) {
            throw new ForbiddenException("STORE_CONTRACT_NOT_STARTED", "Store contract has not started");
        }
        if (end != null && today.isAfter(end)) {
            throw new ForbiddenException("STORE_CONTRACT_EXPIRED", "Store contract has expired");
        }
    }

    private boolean isPlatformAdmin() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(granted -> {
            String authority = granted.getAuthority();
            return "ROLE_ADMIN".equals(authority) || "ROLE_SUPER_ADMIN".equals(authority);
        });
    }

    private UserAccount currentUser() {
        return userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private UserAccount currentUserOptional() {
        return userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail()).orElse(null);
    }

    private ProductRequest requireProductPayload(ProductSubmissionCreateRequest request) {
        if (request == null || request.product() == null) {
            throw new BadRequestException("PRODUCT_REQUIRED", "product payload is required");
        }
        return request.product();
    }

    private void validateStoreScopedPayload(UUID storeId, ProductRequest product) {
        if (product.inventory() != null) {
            List<UUID> storeIds = product.inventory().stream()
                    .filter(item -> item != null)
                    .map(ProductInventoryRequest::storeId)
                    .distinct()
                    .toList();
            for (UUID id : storeIds) {
                if (id != null && !id.equals(storeId)) {
                    throw new BadRequestException("SUBMISSION_STORE_SCOPE", "Submission inventory must only reference the submitting store");
                }
            }
        }
    }

    private Map<String, Object> asMap(Object value) {
        return objectMapper.convertValue(value, new TypeReference<LinkedHashMap<String, Object>>() {
        });
    }

    private <T> T fromMap(Map<String, Object> payload, Class<T> type) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        return objectMapper.convertValue(payload, type);
    }

    private String normalizeProductName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = normalized.replaceAll("[^a-z0-9\\s]+", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String buildDedupeFingerprint(ProductRequest request, String normalizedName) {
        if (request == null) {
            return null;
        }
        String barcode = trimToNull(request.barcode());
        String mpn = trimToNull(request.manufacturerPartNumber());
        String brand = resolveBrandName(request.brand());
        String category = resolveCategoryName(request.categoryId(), request.category());
        String seed = (normalizedName == null ? "" : normalizedName)
                + "|" + (barcode == null ? "" : barcode.toLowerCase(Locale.ROOT))
                + "|" + (mpn == null ? "" : mpn.toLowerCase(Locale.ROOT))
                + "|" + (brand == null ? "" : brand)
                + "|" + (category == null ? "" : category);
        return sha256Hex(seed);
    }

    private String resolveBrandName(String brandInput) {
        String brand = trimToNull(brandInput);
        if (brand == null) {
            return "";
        }
        Brand persisted = brandRepository.findByNameIgnoreCase(brand).orElse(null);
        String value = persisted == null ? brand : persisted.getName();
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveCategoryName(UUID categoryId, String categoryInput) {
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null && category.getName() != null) {
                return category.getName().trim().toLowerCase(Locale.ROOT);
            }
        }
        String category = trimToNull(categoryInput);
        return category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record CandidateAccumulator(Product product, BigDecimal score, String reason, Map<String, Object> detail) {
    }
}
