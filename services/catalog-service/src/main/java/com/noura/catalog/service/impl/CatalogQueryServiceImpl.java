package com.noura.catalog.service.impl;

import com.noura.catalog.domain.entity.CatalogBrand;
import com.noura.catalog.domain.entity.CatalogCategory;
import com.noura.catalog.domain.entity.CatalogProduct;
import com.noura.catalog.domain.entity.CatalogProductInventory;
import com.noura.catalog.domain.entity.CatalogProductMedia;
import com.noura.catalog.domain.entity.CatalogProductVariant;
import com.noura.catalog.domain.entity.CatalogStore;
import com.noura.catalog.domain.enums.ProductStatus;
import com.noura.catalog.dto.catalog.CategoryTreeDto;
import com.noura.catalog.dto.product.ProductDto;
import com.noura.catalog.dto.product.ProductMediaDto;
import com.noura.catalog.dto.product.ProductSearchResultDto;
import com.noura.catalog.dto.product.ProductSeoDto;
import com.noura.catalog.dto.product.ProductStoreInventoryDto;
import com.noura.catalog.dto.product.ProductVariantDto;
import com.noura.catalog.dto.product.TrendTagDto;
import com.noura.catalog.dto.product.VariantLookupResponse;
import com.noura.catalog.exception.NotFoundException;
import com.noura.catalog.repository.CatalogBrandRepository;
import com.noura.catalog.repository.CatalogCategoryRepository;
import com.noura.catalog.repository.CatalogProductInventoryRepository;
import com.noura.catalog.repository.CatalogProductMediaRepository;
import com.noura.catalog.repository.CatalogProductRepository;
import com.noura.catalog.repository.CatalogProductVariantRepository;
import com.noura.catalog.repository.CatalogStoreRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogQueryServiceImpl implements com.noura.catalog.service.CatalogQueryService {

    private final CatalogProductRepository productRepository;
    private final CatalogCategoryRepository categoryRepository;
    private final CatalogBrandRepository brandRepository;
    private final CatalogProductVariantRepository variantRepository;
    private final CatalogProductMediaRepository mediaRepository;
    private final CatalogProductInventoryRepository inventoryRepository;
    private final CatalogStoreRepository storeRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> listProducts(
            String query,
            String category,
            UUID categoryId,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            Boolean flashSale,
            Boolean trending,
            Pageable pageable
    ) {
        List<UUID> categoryIds = resolveCategoryIds(category, categoryId);
        List<UUID> brandIds = resolveBrandIds(brand);

        if (category != null && !category.isBlank() && categoryIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        if (brand != null && !brand.isBlank() && brandIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Specification<CatalogProduct> spec = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("productCode")), like)
                ));
            }

            if (!categoryIds.isEmpty()) {
                predicates.add(root.get("categoryId").in(categoryIds));
            }

            if (!brandIds.isEmpty()) {
                predicates.add(root.get("brandId").in(brandIds));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
            }
            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), minRating));
            }
            if (flashSale != null) {
                predicates.add(cb.equal(root.get("flashSale"), flashSale));
            }
            if (trending != null) {
                predicates.add(cb.equal(root.get("trending"), trending));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<CatalogProduct> products = productRepository.findAll(spec, pageable);
        return products.map(product -> mapProduct(product, null, null, null, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto getProduct(UUID productId) {
        CatalogProduct product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        return mapProduct(product, null, null, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductStoreInventoryDto> productInventory(UUID productId) {
        if (productRepository.findByIdAndActiveTrue(productId).isEmpty()) {
            throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
        }
        List<CatalogProductInventory> inventory = inventoryRepository.findByProductId(productId);
        Map<UUID, String> storeNames = storeNameMap(
                inventory.stream().map(CatalogProductInventory::getStoreId).collect(Collectors.toSet())
        );
        return inventory.stream()
                .map(item -> mapInventory(item, storeNames))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeDto> categoryTree() {
        List<CatalogCategory> categories = categoryRepository.findAllByActiveTrueOrderByLevelAscNameAsc();
        Map<UUID, CategoryTreeNode> nodeById = new LinkedHashMap<>();
        for (CatalogCategory category : categories) {
            nodeById.put(category.getId(), new CategoryTreeNode(category));
        }

        List<CategoryTreeNode> roots = new ArrayList<>();
        for (CatalogCategory category : categories) {
            CategoryTreeNode node = nodeById.get(category.getId());
            UUID parentId = category.getParentId();
            if (parentId != null && nodeById.containsKey(parentId)) {
                nodeById.get(parentId).children.add(node);
            } else {
                roots.add(node);
            }
        }

        return roots.stream()
                .sorted(Comparator.comparing(node -> node.source.getName(), String.CASE_INSENSITIVE_ORDER))
                .map(this::toCategoryTreeDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSearchResultDto> searchProducts(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<CatalogProduct> products = productRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(query.trim());
        Map<UUID, String> categoryNames = categoryNameMap(
                products.stream().map(CatalogProduct::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet())
        );
        return products.stream()
                .map(product -> new ProductSearchResultDto(
                        product.getId(),
                        product.getName(),
                        categoryNames.get(product.getCategoryId()),
                        isBlank(product.getShortDescription()) && isBlank(product.getLongDescription()),
                        isBlank(product.getBarcode()),
                        isBlank(product.getQrCode()),
                        "LEGACY_REUSED"
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrendTagDto> trendTags() {
        List<CatalogProduct> products = productRepository.findTop20ByActiveTrueAndTrendingTrueOrderByPopularityScoreDesc();
        if (products.isEmpty()) {
            return List.of(
                    new TrendTagDto("Featured", 100),
                    new TrendTagDto("Popular", 90),
                    new TrendTagDto("New", 80)
            );
        }

        Map<UUID, String> categoryNames = categoryNameMap(
                products.stream().map(CatalogProduct::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet())
        );

        Map<String, Integer> scoreByTag = new LinkedHashMap<>();
        for (CatalogProduct product : products) {
            String tag = categoryNames.get(product.getCategoryId());
            if (tag == null || tag.isBlank()) {
                tag = "Trending";
            }
            scoreByTag.merge(tag, Math.max(1, product.getPopularityScore()), Integer::sum);
        }

        return scoreByTag.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> new TrendTagDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> bestSellerRecommendations(int limit) {
        List<ProductDto> items = recommendationSlice(
                (root, ignored, cb) -> cb.and(
                        cb.isTrue(root.get("active")),
                        cb.isTrue(root.get("bestSeller"))
                ),
                Sort.by(Sort.Order.desc("popularityScore"), Sort.Order.desc("averageRating"), Sort.Order.asc("name")),
                limit
        );
        return items.isEmpty() ? popularFallback(limit) : items;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> trendingRecommendations(int limit) {
        List<ProductDto> items = recommendationSlice(
                (root, ignored, cb) -> cb.and(
                        cb.isTrue(root.get("active")),
                        cb.isTrue(root.get("trending"))
                ),
                Sort.by(Sort.Order.desc("popularityScore"), Sort.Order.desc("averageRating"), Sort.Order.asc("name")),
                limit
        );
        return items.isEmpty() ? popularFallback(limit) : items;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> dealRecommendations(int limit) {
        List<ProductDto> items = recommendationSlice(
                (root, ignored, cb) -> cb.and(
                        cb.isTrue(root.get("active")),
                        cb.isTrue(root.get("flashSale"))
                ),
                Sort.by(Sort.Order.desc("popularityScore"), Sort.Order.asc("basePrice"), Sort.Order.asc("name")),
                limit
        );
        return items.isEmpty() ? popularFallback(limit) : items;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> personalizedRecommendations(String customerRef, int limit) {
        return rotateDeterministically(popularFallback(limit * 2), customerRef, normalizeLimit(limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> crossSellRecommendations(String customerRef, int limit) {
        List<ProductDto> pool = new ArrayList<>();
        pool.addAll(bestSellerRecommendations(Math.max(normalizeLimit(limit), 6)));
        pool.addAll(trendingRecommendations(Math.max(normalizeLimit(limit), 6)));
        return rotateDeterministically(deduplicate(pool), "cross-sell:" + normalizeNullable(customerRef), normalizeLimit(limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> relatedProducts(UUID productId, int limit) {
        CatalogProduct anchor = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        List<ProductDto> primary = recommendationSlice(
                (root, ignored, cb) -> cb.and(
                        cb.isTrue(root.get("active")),
                        cb.notEqual(root.get("id"), productId),
                        anchor.getCategoryId() == null
                                ? cb.conjunction()
                                : cb.equal(root.get("categoryId"), anchor.getCategoryId())
                ),
                Sort.by(Sort.Order.desc("popularityScore"), Sort.Order.desc("averageRating"), Sort.Order.asc("name")),
                limit
        );
        if (!primary.isEmpty() || anchor.getBrandId() == null) {
            return primary;
        }
        return recommendationSlice(
                (root, ignored, cb) -> cb.and(
                        cb.isTrue(root.get("active")),
                        cb.notEqual(root.get("id"), productId),
                        cb.equal(root.get("brandId"), anchor.getBrandId())
                ),
                Sort.by(Sort.Order.desc("popularityScore"), Sort.Order.desc("averageRating"), Sort.Order.asc("name")),
                limit
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> frequentlyBoughtTogether(UUID productId, int limit) {
        CatalogProduct anchor = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        List<ProductDto> pool = new ArrayList<>();
        pool.addAll(recommendationSlice(
                (root, ignored, cb) -> cb.and(
                        cb.isTrue(root.get("active")),
                        cb.notEqual(root.get("id"), productId),
                        cb.isTrue(root.get("bestSeller")),
                        anchor.getCategoryId() == null
                                ? cb.conjunction()
                                : cb.equal(root.get("categoryId"), anchor.getCategoryId())
                ),
                Sort.by(Sort.Order.desc("popularityScore"), Sort.Order.desc("reviewCount"), Sort.Order.asc("name")),
                Math.max(normalizeLimit(limit), 6)
        ));
        if (anchor.getBrandId() != null) {
            pool.addAll(recommendationSlice(
                    (root, ignored, cb) -> cb.and(
                            cb.isTrue(root.get("active")),
                            cb.notEqual(root.get("id"), productId),
                            cb.equal(root.get("brandId"), anchor.getBrandId())
                    ),
                    Sort.by(Sort.Order.desc("popularityScore"), Sort.Order.desc("averageRating"), Sort.Order.asc("name")),
                    Math.max(normalizeLimit(limit), 6)
            ));
        }
        return deduplicate(pool).stream()
                .limit(normalizeLimit(limit))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VariantLookupResponse lookupVariant(UUID variantId) {
        CatalogProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Variant not found"));
        return new VariantLookupResponse(
                variant.getId(),
                variant.getProductId(),
                variant.getSku(),
                variant.getVariantName(),
                variant.isActive()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSearchResultDto> searchProducts(
            String keyword,
            UUID categoryId,
            UUID brandId,
            ProductStatus status,
            Pageable pageable
    ) {
        Pageable normalizedPageable = normalizeSort(pageable, List.of("createdAt", "updatedAt", "name", "productCode", "status"));
        if (keyword == null || keyword.isBlank()) {
            Page<CatalogProduct> products = productRepository.findAll(normalizedPageable);
            return mapToSearchPage(products);
        }

        Specification<CatalogProduct> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("productCode")), like)
            ));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (brandId != null) {
                predicates.add(cb.equal(root.get("brandId"), brandId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<CatalogProduct> products = productRepository.findAll(spec, normalizedPageable);
        return mapToSearchPage(products);
    }

    private List<ProductDto> recommendationSlice(Specification<CatalogProduct> specification, Sort sort, int limit) {
        int safeLimit = normalizeLimit(limit);
        Page<CatalogProduct> page = productRepository.findAll(specification, PageRequest.of(0, safeLimit, sort));
        return page.getContent().stream()
                .map(product -> mapProduct(product, null, null, null, null, null))
                .toList();
    }

    private List<ProductDto> popularFallback(int limit) {
        return recommendationSlice(
                (root, ignored, cb) -> cb.isTrue(root.get("active")),
                Sort.by(Sort.Order.desc("popularityScore"), Sort.Order.desc("averageRating"), Sort.Order.asc("name")),
                limit
        );
    }

    private List<ProductDto> rotateDeterministically(List<ProductDto> source, String seed, int limit) {
        if (source.isEmpty()) {
            return List.of();
        }
        int safeLimit = Math.min(limit, source.size());
        int offset = 0;
        String normalizedSeed = normalizeNullable(seed);
        if (normalizedSeed != null) {
            offset = Math.floorMod(normalizedSeed.hashCode(), source.size());
        }
        List<ProductDto> rotated = new ArrayList<>(source.size());
        for (int index = 0; index < source.size(); index++) {
            rotated.add(source.get((index + offset) % source.size()));
        }
        return rotated.subList(0, safeLimit);
    }

    private List<ProductDto> deduplicate(List<ProductDto> source) {
        Map<UUID, ProductDto> unique = new LinkedHashMap<>();
        for (ProductDto item : source) {
            if (item != null && item.id() != null) {
                unique.putIfAbsent(item.id(), item);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 24));
    }

    private ProductDto mapProduct(
            CatalogProduct product,
            Map<UUID, List<CatalogProductVariant>> variantMap,
            Map<UUID, List<CatalogProductMedia>> mediaMap,
            Map<UUID, List<CatalogProductInventory>> inventoryMap,
            Map<UUID, String> categoryNames,
            Map<UUID, String> brandNames
    ) {
        List<CatalogProductVariant> variants = variantMap != null
                ? variantMap.getOrDefault(product.getId(), List.of())
                : variantRepository.findByProductIdInAndActiveTrueOrderByProductIdAscSkuAsc(List.of(product.getId()));

        List<CatalogProductMedia> media = mediaMap != null
                ? mediaMap.getOrDefault(product.getId(), List.of())
                : mediaRepository.findByProductIdInOrderByProductIdAscSortOrderAsc(List.of(product.getId()));

        List<CatalogProductInventory> inventory = inventoryMap != null
                ? inventoryMap.getOrDefault(product.getId(), List.of())
                : inventoryRepository.findByProductId(product.getId());

        Map<UUID, String> resolvedCategoryNames = categoryNames != null ? categoryNames :
                categoryNameMap(product.getCategoryId() == null ? List.of() : List.of(product.getCategoryId()));
        Map<UUID, String> resolvedBrandNames = brandNames != null ? brandNames :
                brandNameMap(product.getBrandId() == null ? List.of() : List.of(product.getBrandId()));
        Map<UUID, String> storeNames = storeNameMap(
                inventory.stream().map(CatalogProductInventory::getStoreId).collect(Collectors.toSet())
        );

        return new ProductDto(
                product.getId(),
                product.getName(),
                resolvedCategoryNames.get(product.getCategoryId()),
                resolvedBrandNames.get(product.getBrandId()),
                product.getBasePrice(),
                product.isFlashSale(),
                product.isTrending(),
                product.isBestSeller(),
                product.getAverageRating(),
                product.getReviewCount(),
                product.getPopularityScore(),
                product.getShortDescription(),
                product.getLongDescription(),
                product.getSeoTitle(),
                product.getSeoDescription(),
                product.getSeoSlug(),
                new ProductSeoDto(product.getSeoSlug(), product.getSeoTitle(), product.getSeoDescription()),
                product.getAttributes() == null ? Map.of() : product.getAttributes(),
                product.getStatus(),
                product.isActive(),
                product.isAllowBackorder(),
                variants.stream().map(this::mapVariant).toList(),
                media.stream().map(this::mapMedia).toList(),
                inventory.stream().map(item -> mapInventory(item, storeNames)).toList(),
                product.getLongDescription(),
                product.getTargetAudience(),
                product.getBarcode(),
                product.getQrCode()
        );
    }

    private ProductVariantDto mapVariant(CatalogProductVariant variant) {
        return new ProductVariantDto(
                variant.getId(),
                variant.getColor(),
                variant.getSize(),
                variant.getSku(),
                variant.getAttributes() == null ? Map.of() : variant.getAttributes(),
                variant.getPriceOverride(),
                variant.getStock(),
                variant.isActive()
        );
    }

    private ProductMediaDto mapMedia(CatalogProductMedia media) {
        return new ProductMediaDto(
                media.getId(),
                media.getMediaType(),
                media.getUrl(),
                media.getSortOrder(),
                media.isPrimary()
        );
    }

    private ProductStoreInventoryDto mapInventory(CatalogProductInventory inventory, Map<UUID, String> storeNames) {
        return new ProductStoreInventoryDto(
                inventory.getStoreId(),
                storeNames.get(inventory.getStoreId()),
                inventory.getStock(),
                inventory.getStorePrice(),
                inventory.isPublished(),
                inventory.isVisible(),
                inventory.getLocalName()
        );
    }

    private List<UUID> resolveCategoryIds(String category, UUID categoryId) {
        if (categoryId != null) {
            return List.of(categoryId);
        }
        if (category == null || category.isBlank()) {
            return List.of();
        }
        return categoryRepository.findByNameContainingIgnoreCase(category.trim()).stream()
                .map(CatalogCategory::getId)
                .toList();
    }

    private List<UUID> resolveBrandIds(String brand) {
        if (brand == null || brand.isBlank()) {
            return List.of();
        }
        return brandRepository.findByNameContainingIgnoreCase(brand.trim()).stream()
                .map(CatalogBrand::getId)
                .toList();
    }

    private Map<UUID, String> categoryNameMap(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return categoryRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(CatalogCategory::getId, CatalogCategory::getName, (left, right) -> left, HashMap::new));
    }

    private Map<UUID, String> brandNameMap(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return brandRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(CatalogBrand::getId, CatalogBrand::getName, (left, right) -> left, HashMap::new));
    }

    private Map<UUID, String> storeNameMap(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return storeRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(CatalogStore::getId, CatalogStore::getName, (left, right) -> left, HashMap::new));
    }

    private Page<ProductSearchResultDto> mapToSearchPage(Page<CatalogProduct> products) {
        if (products.isEmpty()) {
            return products.map(product -> mapSearchResult(product));
        }

        Map<UUID, String> categoryNames = categoryNameMap(
                products.getContent().stream().map(CatalogProduct::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet())
        );
        return products.map(product -> mapSearchResult(product, categoryNames));
    }

    private ProductSearchResultDto mapSearchResult(CatalogProduct product) {
        return mapSearchResult(product, Map.of());
    }

    private ProductSearchResultDto mapSearchResult(CatalogProduct product, Map<UUID, String> categoryNames) {
        return new ProductSearchResultDto(
                product.getId(),
                product.getName(),
                product.getCategoryId() == null ? null : categoryNames.get(product.getCategoryId()),
                isBlank(product.getShortDescription()) && isBlank(product.getLongDescription()),
                isBlank(product.getBarcode()),
                isBlank(product.getQrCode()),
                product.getStatus() == null ? null : product.getStatus().name()
        );
    }

    private Pageable normalizeSort(Pageable pageable, List<String> allowedSorts) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"));
        }

        String requestedSort = pageable.getSort().isSorted()
                ? pageable.getSort().stream().findFirst().map(order -> order.getProperty()).orElse("updatedAt")
                : "updatedAt";

        Sort.Direction direction = pageable.getSort().isSorted()
                ? pageable.getSort().stream().findFirst().map(order -> order.getDirection()).orElse(Sort.Direction.DESC)
                : Sort.Direction.DESC;

        String sortField = allowedSorts.contains(requestedSort) ? requestedSort : "updatedAt";
        return PageRequest.of(
                pageable.getPageNumber(),
                Math.max(1, pageable.getPageSize()),
                Sort.by(direction, sortField)
        );
    }

    private CategoryTreeDto toCategoryTreeDto(CategoryTreeNode node) {
        List<CategoryTreeDto> children = node.children.stream()
                .sorted(Comparator.comparing(child -> child.source.getName(), String.CASE_INSENSITIVE_ORDER))
                .map(this::toCategoryTreeDto)
                .toList();
        return new CategoryTreeDto(
                node.source.getId(),
                node.source.getName(),
                node.source.getDescription(),
                node.source.getClassificationCode(),
                node.source.getManagerId(),
                children
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static final class CategoryTreeNode {
        private final CatalogCategory source;
        private final List<CategoryTreeNode> children = new ArrayList<>();

        private CategoryTreeNode(CatalogCategory source) {
            this.source = source;
        }
    }
}
