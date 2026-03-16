package com.noura.platform.service.impl;

import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductVariant;
import com.noura.platform.dto.product.ProductSearchRequest;
import com.noura.platform.dto.product.ProductSearchResponse;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.ProductVariantRepository;
import com.noura.platform.search.ProductSearchAdapter;
import com.noura.platform.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private static final List<String> ALLOWED_SORTS = List.of("createdAt", "updatedAt", "name", "productCode", "status");

    private final ProductSearchAdapter productSearchAdapter;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSearchResponse> searchPublic(ProductSearchRequest request) {
        return search(request, false);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PRODUCT_MANAGER')")
    public Page<ProductSearchResponse> searchAdmin(ProductSearchRequest request) {
        return search(request, true);
    }

    private Page<ProductSearchResponse> search(ProductSearchRequest request, boolean adminView) {
        Pageable pageable = toPageable(request);
        Page<UUID> productIds = productSearchAdapter.searchProductIds(request, pageable, adminView);
        if (productIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> orderedIds = productIds.getContent();
        Map<UUID, Product> productsById = productRepository.findByIdIn(orderedIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left, LinkedHashMap::new));
        Map<UUID, List<String>> skusByProductId = mapSkusByProductId(orderedIds);

        List<ProductSearchResponse> content = orderedIds.stream()
                .map(productsById::get)
                .filter(product -> product != null)
                .map(product -> toResponse(product, skusByProductId.getOrDefault(product.getId(), List.of())))
                .toList();

        return new PageImpl<>(content, pageable, productIds.getTotalElements());
    }

    private Pageable toPageable(ProductSearchRequest request) {
        String sortBy = ALLOWED_SORTS.contains(request.sortBy()) ? request.sortBy() : "updatedAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(request.direction()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(request.page(), request.size(), Sort.by(direction, sortBy));
    }

    private Map<UUID, List<String>> mapSkusByProductId(Collection<UUID> productIds) {
        return productVariantRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(
                        ProductVariant::getProductId,
                        Collectors.mapping(ProductVariant::getSku, Collectors.toList())
                ));
    }

    private ProductSearchResponse toResponse(Product product, List<String> skus) {
        return new ProductSearchResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getSlug(),
                product.getCategoryId(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getBrandId(),
                product.getBrand() == null ? null : product.getBrand().getName(),
                product.getStatus(),
                product.getApprovalStatus(),
                product.isActive(),
                product.getBarcode(),
                skus,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
