package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.ForbiddenException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductInventory;
import com.noura.platform.domain.entity.Store;
import com.noura.platform.domain.entity.StoreTenant;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.entity.UserStoreAssignment;
import com.noura.platform.domain.enums.MerchantContractStatus;
import com.noura.platform.domain.enums.ProductStatus;
import com.noura.platform.domain.enums.StoreTenantStatus;
import com.noura.platform.dto.product.ProductDto;
import com.noura.platform.dto.product.ProductInventoryDto;
import com.noura.platform.dto.product.StoreProductAdoptionRequest;
import com.noura.platform.mapper.ProductMapper;
import com.noura.platform.repository.ProductInventoryRepository;
import com.noura.platform.repository.ProductMediaRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.ProductVariantRepository;
import com.noura.platform.repository.StoreRepository;
import com.noura.platform.repository.StoreTenantRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.repository.UserStoreAssignmentRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.StoreCatalogService;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreCatalogServiceImpl implements StoreCatalogService {

    private final StoreRepository storeRepository;
    private final StoreTenantRepository storeTenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserStoreAssignmentRepository userStoreAssignmentRepository;
    private final ProductRepository productRepository;
    private final ProductInventoryRepository productInventoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductMediaRepository productMediaRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER','MANAGER','PRODUCT_MANAGER')")
    public Page<ProductDto> searchAdoptableMasterProducts(UUID storeId, String query, Pageable pageable) {
        requireStoreAccess(storeId);
        requireStoreContractValid(storeId);
        Specification<Product> spec = buildAdoptableSpec(storeId, query);
        return productRepository.findAll(spec, pageable).map(this::mapRichProduct);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER','MANAGER','PRODUCT_MANAGER')")
    public ProductInventoryDto adoptMasterProduct(UUID storeId, UUID masterProductId, StoreProductAdoptionRequest request) {
        requireStoreAccess(storeId);
        requireStoreContractValid(storeId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        Product product = productRepository.findByIdAndActiveTrue(masterProductId)
                .orElseThrow(() -> new NotFoundException("MASTER_PRODUCT_NOT_FOUND", "Master product not found"));
        if (product.getStatus() != ProductStatus.APPROVED && product.getStatus() != ProductStatus.PUBLISHED) {
            throw new BadRequestException("MASTER_PRODUCT_NOT_APPROVED", "Master product is not approved for adoption");
        }

        ProductInventory inventory = productInventoryRepository.findByProductIdAndStoreId(masterProductId, storeId)
                .orElseGet(() -> {
                    ProductInventory created = new ProductInventory();
                    created.setProduct(product);
                    created.setStore(store);
                    return created;
                });
        applyAdoption(inventory, request, true);
        ProductInventory saved = productInventoryRepository.save(inventory);
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','STORE_MANAGER','MANAGER','PRODUCT_MANAGER')")
    public ProductInventoryDto updateStoreProduct(UUID storeId, UUID masterProductId, StoreProductAdoptionRequest request) {
        requireStoreAccess(storeId);
        requireStoreContractValid(storeId);
        ProductInventory inventory = productInventoryRepository.findByProductIdAndStoreId(masterProductId, storeId)
                .orElseThrow(() -> new NotFoundException("STORE_PRODUCT_NOT_FOUND", "Store product adoption not found"));
        applyAdoption(inventory, request, false);
        return toDto(productInventoryRepository.save(inventory));
    }

    private void applyAdoption(ProductInventory inventory, StoreProductAdoptionRequest request, boolean requirePrice) {
        if (request == null) {
            throw new BadRequestException("ADOPTION_REQUIRED", "Adoption payload is required");
        }
        if (requirePrice && request.storePrice() == null) {
            throw new BadRequestException("STORE_PRICE_REQUIRED", "storePrice is required");
        }
        if (request.storePrice() != null) {
            inventory.setStorePrice(request.storePrice());
        }
        if (request.stock() != null) {
            inventory.setStock(Math.max(0, request.stock()));
        }
        if (request.published() != null) {
            inventory.setPublished(request.published());
        }
        if (request.visible() != null) {
            inventory.setVisible(request.visible());
        }
        if (request.localName() != null) {
            inventory.setLocalName(request.localName().isBlank() ? null : request.localName().trim());
        }
        if (request.localDescription() != null) {
            inventory.setLocalDescription(request.localDescription().isBlank() ? null : request.localDescription().trim());
        }
        if (request.taxCode() != null) {
            inventory.setTaxCode(request.taxCode().isBlank() ? null : request.taxCode().trim());
        }
    }

    private Specification<Product> buildAdoptableSpec(UUID storeId, String query) {
        return (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            predicates.add(root.get("status").in(List.of(ProductStatus.APPROVED, ProductStatus.PUBLISHED)));

            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("normalizedName")), like),
                        cb.like(cb.lower(root.get("manufacturerPartNumber")), like),
                        cb.like(cb.lower(root.get("barcode")), like)
                ));
            }

            // Exclude products already adopted by this store.
            Subquery<Long> adopted = q.subquery(Long.class);
            Root<ProductInventory> inventoryRoot = adopted.from(ProductInventory.class);
            adopted.select(cb.literal(1L));
            adopted.where(
                    cb.equal(inventoryRoot.get("product").get("id"), root.get("id")),
                    cb.equal(inventoryRoot.get("store").get("id"), storeId)
            );
            predicates.add(cb.not(cb.exists(adopted)));

            return cb.and(predicates.toArray(Predicate[]::new));
        };
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
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
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
        if (tenant.getContract().getStartDate() != null && today.isBefore(tenant.getContract().getStartDate())) {
            throw new ForbiddenException("STORE_CONTRACT_NOT_STARTED", "Store contract has not started");
        }
        if (tenant.getContract().getEndDate() != null && today.isAfter(tenant.getContract().getEndDate())) {
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

    private ProductInventoryDto toDto(ProductInventory inventory) {
        return new ProductInventoryDto(
                inventory.getId(),
                inventory.getProduct().getId(),
                inventory.getStore().getId(),
                inventory.getStock(),
                inventory.getStorePrice(),
                inventory.isPublished(),
                inventory.isVisible(),
                inventory.getLocalName(),
                inventory.getLocalDescription(),
                inventory.getTaxCode()
        );
    }

    private ProductDto mapRichProduct(Product product) {
        ProductDto dto = productMapper.toDto(product);
        var variants = productVariantRepository.findByProductId(product.getId()).stream().map(productMapper::toVariantDto).toList();
        var media = productMediaRepository.findByProductIdOrderBySortOrderAsc(product.getId()).stream().map(productMapper::toMediaDto).toList();
        // Store adoption endpoints return store inventory separately; master product search remains lightweight.
        return new ProductDto(
                dto.id(),
                dto.name(),
                dto.category(),
                dto.brand(),
                dto.price(),
                dto.flashSale(),
                dto.trending(),
                dto.bestSeller(),
                dto.averageRating(),
                dto.reviewCount(),
                dto.popularityScore(),
                dto.shortDescription(),
                dto.longDescription(),
                dto.seoTitle(),
                dto.seoDescription(),
                dto.seoSlug(),
                dto.seo(),
                dto.attributes(),
                dto.status(),
                dto.active(),
                dto.allowBackorder(),
                variants,
                media,
                List.of(),
                dto.description(),
                dto.targetAudience(),
                dto.barcode(),
                dto.qrCode()
        );
    }
}

