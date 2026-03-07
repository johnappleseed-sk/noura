package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.*;
import com.noura.platform.domain.enums.ProductStatus;
import com.noura.platform.dto.product.*;
import com.noura.platform.mapper.ProductMapper;
import com.noura.platform.repository.*;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.ProductService;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductMediaRepository productMediaRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ProductInventoryRepository productInventoryRepository;
    private final StoreRepository storeRepository;
    private final TrendTagRepository trendTagRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProductMapper productMapper;

    /**
     * Creates product.
     *
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products"}, allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDto createProduct(ProductRequest request) {
        Product product = new Product();
        mapProductFields(product, request);
        product = productRepository.save(product);
        syncCatalogArtifacts(product, request);
        return mapRichProduct(product);
    }

    /**
     * Updates product.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products"}, allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDto updateProduct(UUID productId, ProductRequest request) {
        Product product = getActiveProductOrThrow(productId);
        mapProductFields(product, request);
        product = productRepository.save(product);
        syncCatalogArtifacts(product, request);
        return mapRichProduct(product);
    }

    /**
     * Patches product.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products"}, allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDto patchProduct(UUID productId, ProductPatchRequest request) {
        Product product = getActiveProductOrThrow(productId);
        if (request.shortDescription() != null) {
            product.setShortDescription(request.shortDescription());
        }
        if (request.longDescription() != null) {
            product.setLongDescription(request.longDescription());
        }
        if (request.attributes() != null) {
            product.setAttributes(new LinkedHashMap<>(request.attributes()));
        }
        if (request.seo() != null) {
            applySeo(product, request.seo());
        }
        if (request.active() != null) {
            product.setActive(request.active());
            product.setDeletedAt(request.active() ? null : Instant.now());
        }
        if (request.allowBackorder() != null) {
            product.setAllowBackorder(request.allowBackorder());
        }
        return mapRichProduct(productRepository.save(product));
    }

    /**
     * Deletes product.
     *
     * @param productId The product id used to locate the target record.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products"}, allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(UUID productId) {
        Product product = getProductOrThrow(productId);
        product.setActive(false);
        product.setDeletedAt(Instant.now());
        productRepository.save(product);
    }

    /**
     * Retrieves product.
     *
     * @param productId The product id used to locate the target record.
     * @return The mapped DTO representation.
     */
    @Override
    @Cacheable(cacheNames = "products", key = "'product:' + #productId")
    public ProductDto getProduct(UUID productId) {
        return mapRichProduct(getActiveProductOrThrow(productId));
    }

    /**
     * Lists products.
     *
     * @param filter The filter criteria applied to this operation.
     * @param pageable The pagination configuration.
     * @return A paginated result set.
     */
    @Override
    @Cacheable(cacheNames = "products", key = "'list:' + #filter.toString() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ProductDto> listProducts(ProductFilterRequest filter, Pageable pageable) {
        if (filter == null) {
            filter = ProductFilterRequest.builder().build();
        }
        Specification<Product> spec = buildSpec(filter);
        return productRepository.findAll(spec, pageable).map(this::mapRichProduct);
    }

    /**
     * Lists variants.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    @Override
    public List<ProductVariantDto> listVariants(UUID productId) {
        getActiveProductOrThrow(productId);
        return productVariantRepository.findByProductId(productId)
                .stream()
                .map(productMapper::toVariantDto)
                .toList();
    }

    /**
     * Adds variant.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products"}, allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductVariantDto addVariant(UUID productId, ProductVariantRequest request) {
        Product product = getActiveProductOrThrow(productId);
        ProductVariant variant = new ProductVariant();
        Map<String, Object> normalizedAttributes = normalizeVariantAttributes(request);
        variant.setProduct(product);
        variant.setColor(resolveVariantColor(request, normalizedAttributes));
        variant.setSize(resolveVariantSize(request, normalizedAttributes));
        variant.setSku(request.sku());
        variant.setAttributes(normalizedAttributes);
        variant.setPriceOverride(request.price());
        variant.setStock(request.stock() == null ? 0 : request.stock());
        variant.setActive(true);
        return productMapper.toVariantDto(productVariantRepository.save(variant));
    }

    /**
     * Updates variant.
     *
     * @param variantId The variant id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products"}, allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductVariantDto updateVariant(UUID variantId, ProductVariantRequest request) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Variant not found"));
        if (!variant.getProduct().isActive()) {
            throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
        }
        Map<String, Object> normalizedAttributes = normalizeVariantAttributes(request);
        variant.setColor(resolveVariantColor(request, normalizedAttributes));
        variant.setSize(resolveVariantSize(request, normalizedAttributes));
        variant.setSku(request.sku());
        variant.setAttributes(normalizedAttributes);
        variant.setPriceOverride(request.price());
        variant.setStock(request.stock() == null ? 0 : request.stock());
        variant.setActive(true);
        return productMapper.toVariantDto(productVariantRepository.save(variant));
    }

    /**
     * Adds media.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products"}, allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductMediaDto addMedia(UUID productId, ProductMediaRequest request) {
        Product product = getActiveProductOrThrow(productId);
        ProductMedia media = new ProductMedia();
        media.setProduct(product);
        media.setMediaType(request.mediaType());
        media.setUrl(request.url());
        media.setSortOrder(request.sortOrder());
        media.setPrimary(request.isPrimary());
        return productMapper.toMediaDto(productMediaRepository.save(media));
    }

    /**
     * Executes upsert inventory.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products"}, allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductInventoryDto upsertInventory(UUID productId, ProductInventoryRequest request) {
        Product product = getActiveProductOrThrow(productId);
        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
        ProductInventory inventory = productInventoryRepository.findByProductIdAndStoreId(productId, request.storeId())
                .orElseGet(() -> {
                    ProductInventory created = new ProductInventory();
                    created.setProduct(product);
                    created.setStore(store);
                    return created;
                });
        inventory.setStock(request.stock());
        inventory.setStorePrice(request.storePrice());
        ProductInventory saved = productInventoryRepository.save(inventory);
        return new ProductInventoryDto(saved.getId(), productId, store.getId(), saved.getStock(), saved.getStorePrice());
    }

    /**
     * Executes inventories.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    @Override
    public List<ProductInventoryDto> inventories(UUID productId) {
        getActiveProductOrThrow(productId);
        return productInventoryRepository.findByProductId(productId)
                .stream()
                .map(item -> new ProductInventoryDto(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getStore().getId(),
                        item.getStock(),
                        item.getStorePrice()
                ))
                .toList();
    }

    /**
     * Adds review.
     *
     * @param productId The product id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    @Override
    @Transactional
    public ProductReviewDto addReview(UUID productId, ProductReviewRequest request) {
        Product product = getActiveProductOrThrow(productId);
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.rating());
        review.setComment(request.comment());
        ProductReview saved = productReviewRepository.save(review);
        recalculateRating(product);
        return productMapper.toReviewDto(saved);
    }

    /**
     * Executes reviews.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    @Override
    public List<ProductReviewDto> reviews(UUID productId) {
        return productReviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream().map(productMapper::toReviewDto).toList();
    }

    /**
     * Executes related products.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    @Override
    public List<ProductDto> relatedProducts(UUID productId) {
        Product seed = getActiveProductOrThrow(productId);
        return productRepository.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(productId))
                .filter(Product::isActive)
                .filter(candidate ->
                        (seed.getCategory() != null && candidate.getCategory() != null
                                && candidate.getCategory().getId().equals(seed.getCategory().getId()))
                                || (seed.getBrand() != null && candidate.getBrand() != null
                                && candidate.getBrand().getId().equals(seed.getBrand().getId()))
                )
                .sorted(Comparator.comparingInt(Product::getPopularityScore).reversed())
                .limit(6)
                .map(this::mapRichProduct)
                .toList();
    }

    /**
     * Executes frequently bought together.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    @Override
    public List<ProductDto> frequentlyBoughtTogether(UUID productId) {
        return productRepository.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(productId))
                .filter(Product::isActive)
                .sorted(Comparator.comparingInt(Product::getPopularityScore).reversed())
                .limit(6)
                .map(this::mapRichProduct)
                .toList();
    }

    /**
     * Executes trend tags.
     *
     * @return A list of matching items.
     */
    @Override
    public List<TrendTagDto> trendTags() {
        return trendTagRepository.findTop20ByOrderByScoreDesc()
                .stream()
                .map(item -> new TrendTagDto(item.getValue(), item.getScore()))
                .toList();
    }

    /**
     * Transforms data for product fields.
     *
     * @param product The product value.
     * @param request The request payload for this operation.
     */
    private void mapProductFields(Product product, ProductRequest request) {
        Category category = resolveCategory(request);
        Brand brand = resolveBrand(request.brand());
        BigDecimal basePrice = resolveBasePrice(request);
        product.setName(request.name());
        if (request.description() != null && request.shortDescription() == null) {
            product.setShortDescription(request.description());
        }
        product.setCategory(category);
        product.setBrand(brand);
        product.setBasePrice(basePrice);
        product.setAttributes(request.attributes() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.attributes()));
        product.setAllowBackorder(request.allowBackorder());
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.DRAFT);
        }
        if (!product.isActive()) {
            product.setActive(true);
            product.setDeletedAt(null);
        }
        product.setFlashSale(request.flashSale());
        product.setTrending(request.trending());
        product.setBestSeller(request.bestSeller());
        if (request.shortDescription() != null) {
            product.setShortDescription(request.shortDescription());
        }
        if (request.longDescription() != null) {
            product.setLongDescription(request.longDescription());
        }
        if (request.seo() != null) {
            applySeo(product, request.seo());
        } else {
            product.setSeoTitle(request.seoTitle());
            product.setSeoDescription(request.seoDescription());
            product.setSeoSlug(request.seoSlug());
        }
    }

    /**
     * Transforms data for rich product.
     *
     * @param product The product value.
     * @return The mapped DTO representation.
     */
    private ProductDto mapRichProduct(Product product) {
        ProductDto dto = productMapper.toDto(product);
        List<ProductVariantDto> variants = productVariantRepository.findByProductId(product.getId())
                .stream().map(productMapper::toVariantDto).toList();
        List<ProductMediaDto> media = productMediaRepository.findByProductIdOrderBySortOrderAsc(product.getId())
                .stream().map(productMapper::toMediaDto).toList();
        List<ProductStoreInventoryDto> inventory = productInventoryRepository.findByProductId(product.getId())
                .stream()
                .map(item -> new ProductStoreInventoryDto(
                        item.getStore().getId(),
                        item.getStore().getName(),
                        item.getStock(),
                        item.getStorePrice()
                ))
                .toList();
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
                new ProductSeoDto(dto.seoSlug(), dto.seoTitle(), dto.seoDescription()),
                dto.attributes(),
                dto.status(),
                dto.active(),
                dto.allowBackorder(),
                variants,
                media,
                inventory
        );
    }

    /**
     * Builds spec.
     *
     * @param filter The filter criteria applied to this operation.
     * @return The result of build spec.
     */
    private Specification<Product> buildSpec(ProductFilterRequest filter) {
        List<Specification<Product>> specs = new ArrayList<>();
        specs.add((root, query, cb) -> cb.isTrue(root.get("active")));
        if (filter.getQuery() != null && !filter.getQuery().isBlank()) {
            String q = "%" + filter.getQuery().toLowerCase() + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), q),
                    cb.like(cb.lower(root.get("shortDescription")), q),
                    cb.like(cb.lower(root.get("seoSlug")), q)
            ));
        }
        if (filter.getCategoryId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.join("category").get("id"), filter.getCategoryId()));
        }
        if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
            specs.add((root, query, cb) -> cb.equal(cb.lower(root.join("category").get("name")), filter.getCategory().toLowerCase()));
        }
        if (filter.getBrand() != null && !filter.getBrand().isBlank()) {
            specs.add((root, query, cb) -> cb.equal(cb.lower(root.join("brand").get("name")), filter.getBrand().toLowerCase()));
        }
        if (filter.getMinPrice() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("basePrice"), filter.getMinPrice()));
        }
        if (filter.getMaxPrice() != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("basePrice"), filter.getMaxPrice()));
        }
        if (filter.getMinRating() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("averageRating"), filter.getMinRating()));
        }
        if (filter.getFlashSale() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("flashSale"), filter.getFlashSale()));
        }
        if (filter.getTrending() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("trending"), filter.getTrending()));
        }
        if (filter.getAttributeKey() != null
                && !filter.getAttributeKey().isBlank()
                && filter.getAttributeValue() != null
                && !filter.getAttributeValue().isBlank()) {
            String key = filter.getAttributeKey().trim();
            String value = filter.getAttributeValue().trim();
            specs.add((root, query, cb) -> cb.equal(
                    cb.function(
                            "jsonb_extract_path_text",
                            String.class,
                            root.get("attributes"),
                            cb.literal(key)
                    ),
                    value
            ));
        }
        if (filter.getStoreId() != null && Boolean.TRUE.equals(filter.getAvailableAtStore())) {
            specs.add((root, query, cb) -> {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<ProductInventory> inventoryRoot = subquery.from(ProductInventory.class);
                subquery.select(cb.literal(1L));
                subquery.where(
                        cb.equal(inventoryRoot.get("product").get("id"), root.get("id")),
                        cb.equal(inventoryRoot.get("store").get("id"), filter.getStoreId()),
                        cb.greaterThan(inventoryRoot.get("stock"), 0)
                );
                return cb.exists(subquery);
            });
        }
        return specs.stream().reduce(Specification.where(null), Specification::and);
    }

    /**
     * Executes sync catalog artifacts.
     *
     * @param product The product value.
     * @param request The request payload for this operation.
     */
    private void syncCatalogArtifacts(Product product, ProductRequest request) {
        productVariantRepository.deleteByProductId(product.getId());
        if (request.variants() != null) {
            for (ProductVariantRequest variantRequest : request.variants()) {
                ProductVariant variant = new ProductVariant();
                Map<String, Object> normalizedAttributes = normalizeVariantAttributes(variantRequest);
                variant.setProduct(product);
                variant.setColor(resolveVariantColor(variantRequest, normalizedAttributes));
                variant.setSize(resolveVariantSize(variantRequest, normalizedAttributes));
                variant.setSku(variantRequest.sku());
                variant.setAttributes(normalizedAttributes);
                variant.setPriceOverride(variantRequest.price());
                variant.setStock(variantRequest.stock() == null ? 0 : variantRequest.stock());
                variant.setActive(true);
                productVariantRepository.save(variant);
            }
        }

        productMediaRepository.deleteByProductId(product.getId());
        if (request.media() != null) {
            for (ProductMediaRequest mediaRequest : request.media()) {
                ProductMedia media = new ProductMedia();
                media.setProduct(product);
                media.setMediaType(mediaRequest.mediaType());
                media.setUrl(mediaRequest.url());
                media.setSortOrder(mediaRequest.sortOrder());
                media.setPrimary(mediaRequest.isPrimary());
                productMediaRepository.save(media);
            }
        }

        productInventoryRepository.deleteByProductId(product.getId());
        if (request.inventory() != null) {
            for (ProductInventoryRequest inventoryRequest : request.inventory()) {
                Store store = storeRepository.findById(inventoryRequest.storeId())
                        .orElseThrow(() -> new NotFoundException("STORE_NOT_FOUND", "Store not found"));
                ProductInventory inventory = new ProductInventory();
                inventory.setProduct(product);
                inventory.setStore(store);
                inventory.setStock(inventoryRequest.stock());
                inventory.setStorePrice(inventoryRequest.storePrice());
                productInventoryRepository.save(inventory);
            }
        }
    }

    /**
     * Retrieves product or throw.
     *
     * @param productId The product id used to locate the target record.
     * @return The result of get product or throw.
     */
    private Product getProductOrThrow(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
    }

    /**
     * Retrieves active product or throw.
     *
     * @param productId The product id used to locate the target record.
     * @return The result of get active product or throw.
     */
    private Product getActiveProductOrThrow(UUID productId) {
        Product product = getProductOrThrow(productId);
        if (!product.isActive()) {
            throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
        }
        return product;
    }

    /**
     * Resolves category.
     *
     * @param request The request payload for this operation.
     * @return The result of resolve category.
     */
    private Category resolveCategory(ProductRequest request) {
        if (request.categoryId() != null) {
            return categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));
        }
        if (request.category() == null || request.category().isBlank()) {
            throw new BadRequestException("CATEGORY_REQUIRED", "categoryId or category name is required");
        }
        return categoryRepository.findByNameIgnoreCase(request.category())
                .orElseGet(() -> {
                    Category created = new Category();
                    created.setName(request.category().trim());
                    created.setDescription(request.category().trim() + " products");
                    return categoryRepository.save(created);
                });
    }

    /**
     * Resolves brand.
     *
     * @param brandName The brand name value.
     * @return The result of resolve brand.
     */
    private Brand resolveBrand(String brandName) {
        if (brandName == null || brandName.isBlank()) {
            return null;
        }
        return brandRepository.findByNameIgnoreCase(brandName)
                .orElseGet(() -> {
                    Brand created = new Brand();
                    created.setName(brandName.trim());
                    return brandRepository.save(created);
                });
    }

    /**
     * Resolves base price.
     *
     * @param request The request payload for this operation.
     * @return The result of resolve base price.
     */
    private BigDecimal resolveBasePrice(ProductRequest request) {
        if (request.price() != null) {
            return request.price();
        }
        if (request.variants() != null) {
            return request.variants().stream()
                    .map(ProductVariantRequest::price)
                    .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) >= 0)
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(
                            "PRODUCT_PRICE_REQUIRED",
                            "Either product price or at least one variant price is required"
                    ));
        }
        throw new BadRequestException("PRODUCT_PRICE_REQUIRED", "Either product price or variant prices are required");
    }

    /**
     * Applies seo.
     *
     * @param product The product value.
     * @param seo The seo value.
     */
    private void applySeo(Product product, ProductSeoRequest seo) {
        product.setSeoSlug(seo.slug());
        product.setSeoTitle(seo.metaTitle());
        product.setSeoDescription(seo.metaDescription());
    }

    /**
     * Normalizes variant attributes.
     *
     * @param request The request payload for this operation.
     * @return The result of normalize variant attributes.
     */
    private Map<String, Object> normalizeVariantAttributes(ProductVariantRequest request) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (request.attributes() != null) {
            attributes.putAll(request.attributes());
        }
        if (request.color() != null && !request.color().isBlank()) {
            attributes.putIfAbsent("color", request.color());
        }
        if (request.size() != null && !request.size().isBlank()) {
            attributes.putIfAbsent("size", request.size());
        }
        return attributes;
    }

    /**
     * Resolves variant color.
     *
     * @param request The request payload for this operation.
     * @param attributes The attributes value.
     * @return The result of resolve variant color.
     */
    private String resolveVariantColor(ProductVariantRequest request, Map<String, Object> attributes) {
        if (request.color() != null && !request.color().isBlank()) {
            return request.color().trim();
        }
        Object color = attributes.get("color");
        if (color != null && !String.valueOf(color).isBlank()) {
            return String.valueOf(color).trim();
        }
        return "N/A";
    }

    /**
     * Resolves variant size.
     *
     * @param request The request payload for this operation.
     * @param attributes The attributes value.
     * @return The result of resolve variant size.
     */
    private String resolveVariantSize(ProductVariantRequest request, Map<String, Object> attributes) {
        if (request.size() != null && !request.size().isBlank()) {
            return request.size().trim();
        }
        Object size = attributes.get("size");
        if (size != null && !String.valueOf(size).isBlank()) {
            return String.valueOf(size).trim();
        }
        return "N/A";
    }

    /**
     * Executes recalculate rating.
     *
     * @param product The product value.
     */
    private void recalculateRating(Product product) {
        List<ProductReview> reviews = productReviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
        double avg = reviews.stream().mapToInt(ProductReview::getRating).average().orElse(0D);
        product.setAverageRating(avg);
        product.setReviewCount(reviews.size());
        productRepository.save(product);
    }
}
