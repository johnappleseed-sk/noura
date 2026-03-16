package com.noura.platform.search;

import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductVariant;
import com.noura.platform.domain.enums.ProductStatus;
import com.noura.platform.dto.product.ProductSearchRequest;
import com.noura.platform.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostgresProductSearchAdapter implements ProductSearchAdapter {

    private final ProductRepository productRepository;

    @Override
    public Page<UUID> searchProductIds(ProductSearchRequest request, Pageable pageable, boolean adminView) {
        return productRepository.findAll(buildSpecification(request, adminView), pageable)
                .map(Product::getId);
    }

    private Specification<Product> buildSpecification(ProductSearchRequest request, boolean adminView) {
        return (root, query, cb) -> {
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();
            applyVisibilityPredicates(predicates, root, cb, request, adminView);
            applyFilterPredicates(predicates, root, cb, request);
            applyKeywordPredicate(predicates, root, query.subquery(UUID.class), cb, request);

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void applyVisibilityPredicates(
            List<Predicate> predicates,
            Root<Product> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            ProductSearchRequest request,
            boolean adminView
    ) {
        if (adminView) {
            if (request.status() != null) {
                predicates.add(cb.equal(root.get("status"), request.status()));
            }
            return;
        }

        predicates.add(cb.isTrue(root.get("active")));
        if (request.status() == null) {
            predicates.add(root.get("status").in(List.of(ProductStatus.APPROVED, ProductStatus.PUBLISHED)));
            return;
        }

        if (request.status() != ProductStatus.APPROVED && request.status() != ProductStatus.PUBLISHED) {
            predicates.add(cb.disjunction());
            return;
        }

        predicates.add(cb.equal(root.get("status"), request.status()));
    }

    private void applyFilterPredicates(
            List<Predicate> predicates,
            Root<Product> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            ProductSearchRequest request
    ) {
        if (request.categoryId() != null) {
            predicates.add(cb.equal(root.get("categoryId"), request.categoryId()));
        }
        if (request.brandId() != null) {
            predicates.add(cb.equal(root.get("brandId"), request.brandId()));
        }
    }

    private void applyKeywordPredicate(
            List<Predicate> predicates,
            Root<Product> root,
            Subquery<UUID> skuSubquery,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            ProductSearchRequest request
    ) {
        if (request.keyword() == null || request.keyword().isBlank()) {
            return;
        }

        String like = "%" + request.keyword().trim().toLowerCase(Locale.ROOT) + "%";
        Root<ProductVariant> variantRoot = skuSubquery.from(ProductVariant.class);
        skuSubquery.select(variantRoot.get("productId"));
        skuSubquery.where(
                cb.equal(variantRoot.get("productId"), root.get("id")),
                cb.like(cb.lower(variantRoot.get("sku")), like)
        );

        predicates.add(cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("productCode")), like),
                cb.exists(skuSubquery)
        ));
    }
}
