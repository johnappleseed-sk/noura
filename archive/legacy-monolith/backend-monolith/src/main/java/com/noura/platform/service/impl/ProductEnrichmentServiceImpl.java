package com.noura.platform.service.impl;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductGeneratorBridge;
import com.noura.platform.domain.entity.ProductGeneratorMirrorJob;
import com.noura.platform.domain.entity.ProductVariant;
import com.noura.platform.domain.enums.ProductGeneratorMirrorJobStatus;
import com.noura.platform.dto.product.ProductEnrichmentResponse;
import com.noura.platform.dto.product.ProductSearchResultDto;
import com.noura.platform.repository.ProductGeneratorBridgeRepository;
import com.noura.platform.repository.ProductGeneratorMirrorJobRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.ProductVariantRepository;
import com.noura.platform.service.ProductEnrichmentService;
import com.noura.platform.service.impl.productgen.ProductCodeImageService;
import com.noura.platform.service.impl.productgen.ProductDescriptionGenerationService;
import com.noura.platform.service.impl.productgen.ProductDescriptionPrompt;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductEnrichmentServiceImpl implements ProductEnrichmentService {

    private static final int SEARCH_LIMIT = 20;
    private static final Pattern HEX_32_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductGeneratorBridgeRepository productGeneratorBridgeRepository;
    private final ProductGeneratorMirrorJobRepository productGeneratorMirrorJobRepository;
    private final ProductDescriptionGenerationService productDescriptionGenerationService;
    private final ProductCodeImageService productCodeImageService;
    private final AppProperties appProperties;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<ProductSearchResultDto> searchExistingProducts(String q) {
        String query = q == null ? "" : q.trim();
        if (query.isBlank()) {
            return List.of();
        }

        Map<UUID, Product> matches = new LinkedHashMap<>();
        parseUuid(query).ifPresent(productId -> productRepository.findByIdAndActiveTrue(productId)
                .ifPresent(product -> matches.put(product.getId(), product)));

        for (Product product : productRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(query)) {
            matches.putIfAbsent(product.getId(), product);
            if (matches.size() >= SEARCH_LIMIT) {
                break;
            }
        }

        if (matches.size() < SEARCH_LIMIT) {
            List<ProductVariant> variants = productVariantRepository.findTop20BySkuContainingIgnoreCaseOrderByUpdatedAtDesc(query);
            for (ProductVariant variant : variants) {
                Product product = variant.getProduct();
                if (product != null && product.isActive()) {
                    matches.putIfAbsent(product.getId(), product);
                    if (matches.size() >= SEARCH_LIMIT) {
                        break;
                    }
                }
            }
        }

        return matches.values().stream()
                .limit(SEARCH_LIMIT)
                .map(product -> new ProductSearchResultDto(
                        product.getId(),
                        product.getName(),
                        product.getCategory() == null ? null : product.getCategory().getName(),
                        isDescriptionMissing(product),
                        isMissing(product.getBarcode()),
                        isMissing(product.getQrCode()),
                        resolveMirrorStatus(product.getId())
                ))
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductEnrichmentResponse generateMissingFields(UUID productId) {
        Product product = loadActiveProduct(productId);

        boolean descriptionGenerated = false;
        boolean barcodeGenerated = false;
        boolean qrGenerated = false;

        if (isDescriptionMissing(product)) {
            product.setLongDescription(generateDescription(product));
            descriptionGenerated = true;
        }
        if (isMissing(product.getBarcode())) {
            product.setBarcode(productCodeImageService.generateUniqueEan13(productRepository::existsByBarcodeIgnoreCase));
            barcodeGenerated = true;
        }
        if (isMissing(product.getQrCode())) {
            product.setQrCode(resolveQrPayload(product));
            qrGenerated = true;
        }

        if (descriptionGenerated || barcodeGenerated || qrGenerated) {
            productRepository.save(product);
        }

        MirrorQueueResult mirror = queueMirrorJob(product, descriptionGenerated, barcodeGenerated, qrGenerated);
        return toResponse(product, descriptionGenerated, barcodeGenerated, qrGenerated, mirror);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductEnrichmentResponse generateDescription(UUID productId) {
        Product product = loadActiveProduct(productId);
        boolean generated = false;
        if (isDescriptionMissing(product)) {
            product.setLongDescription(generateDescription(product));
            generated = true;
            productRepository.save(product);
        }
        MirrorQueueResult mirror = queueMirrorJob(product, generated, false, false);
        return toResponse(product, generated, false, false, mirror);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductEnrichmentResponse generateBarcode(UUID productId) {
        Product product = loadActiveProduct(productId);
        boolean generated = false;
        if (isMissing(product.getBarcode())) {
            product.setBarcode(productCodeImageService.generateUniqueEan13(productRepository::existsByBarcodeIgnoreCase));
            generated = true;
            productRepository.save(product);
        }
        MirrorQueueResult mirror = queueMirrorJob(product, false, generated, false);
        return toResponse(product, false, generated, false, mirror);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductEnrichmentResponse generateQr(UUID productId) {
        Product product = loadActiveProduct(productId);
        boolean generated = false;
        if (isMissing(product.getQrCode())) {
            product.setQrCode(resolveQrPayload(product));
            generated = true;
            productRepository.save(product);
        }
        MirrorQueueResult mirror = queueMirrorJob(product, false, false, generated);
        return toResponse(product, false, false, generated, mirror);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public byte[] barcodeImage(UUID productId) {
        Product product = loadActiveProduct(productId);
        if (isMissing(product.getBarcode())) {
            throw new NotFoundException("PRODUCT_BARCODE_NOT_FOUND", "Product barcode is not available.");
        }
        return productCodeImageService.barcodePng(product.getBarcode());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public byte[] qrImage(UUID productId) {
        Product product = loadActiveProduct(productId);
        if (isMissing(product.getQrCode())) {
            throw new NotFoundException("PRODUCT_QR_NOT_FOUND", "Product QR code is not available.");
        }
        return productCodeImageService.qrPng(product.getQrCode());
    }

    private Product loadActiveProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        if (!product.isActive()) {
            throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
        }
        return product;
    }

    private String generateDescription(Product product) {
        ProductDescriptionPrompt prompt = new ProductDescriptionPrompt(
                defaultText(product.getName(), "Unknown product"),
                defaultText(product.getCategory() == null ? null : product.getCategory().getName(), "General"),
                defaultText(product.getBrand() == null ? null : product.getBrand().getName(), "Noura"),
                defaultText(product.getTargetAudience(), "online shoppers")
        );
        return productDescriptionGenerationService.generate(prompt);
    }

    private String resolveQrPayload(Product product) {
        String template = appProperties.getProductGenerator().getProductUrlTemplate();
        String resolvedTemplate = StringUtils.hasText(template)
                ? template
                : "https://store.example.com/products/{id}";
        return resolvedTemplate.replace("{id}", product.getId().toString());
    }

    private MirrorQueueResult queueMirrorJob(
            Product product,
            boolean descriptionUpdated,
            boolean barcodeUpdated,
            boolean qrUpdated
    ) {
        if (!descriptionUpdated && !barcodeUpdated && !qrUpdated) {
            return new MirrorQueueResult(resolveMirrorStatus(product.getId()), null);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        if (descriptionUpdated) {
            payload.put("description", resolveDescription(product));
        }
        if (barcodeUpdated) {
            payload.put("barcode", product.getBarcode());
        }
        if (qrUpdated) {
            payload.put("qrCode", product.getQrCode());
        }

        ProductGeneratorMirrorJob job = new ProductGeneratorMirrorJob();
        job.setCommerceProduct(product);
        job.setPayload(payload);
        job.setAttempts(0);
        job.setMaxAttempts(Math.max(1, appProperties.getProductGenerator().getMirror().getMaxAttempts()));

        ProductGeneratorBridge bridge = productGeneratorBridgeRepository.findByCommerceProduct_Id(product.getId()).orElse(null);
        if (bridge == null) {
            job.setStatus(ProductGeneratorMirrorJobStatus.BLOCKED_MAPPING);
            job.setLastError("No inventory mapping found for this commerce product.");
            productGeneratorMirrorJobRepository.save(job);
            return new MirrorQueueResult(
                    ProductGeneratorMirrorJobStatus.BLOCKED_MAPPING.name(),
                    "Inventory sync is blocked: no inventory mapping exists for this product."
            );
        }

        job.setStatus(ProductGeneratorMirrorJobStatus.PENDING);
        job.setNextRetryAt(Instant.now());
        productGeneratorMirrorJobRepository.save(job);
        return new MirrorQueueResult(ProductGeneratorMirrorJobStatus.PENDING.name(), null);
    }

    private ProductEnrichmentResponse toResponse(
            Product product,
            boolean descriptionGenerated,
            boolean barcodeGenerated,
            boolean qrGenerated,
            MirrorQueueResult mirror
    ) {
        String baseApiPath = appProperties.getApi().getVersionPrefix() + "/products/" + product.getId();
        String barcodeImageUrl = isMissing(product.getBarcode()) ? null : baseApiPath + "/barcode-image";
        String qrImageUrl = isMissing(product.getQrCode()) ? null : baseApiPath + "/qr-image";

        return new ProductEnrichmentResponse(
                product.getId(),
                product.getName(),
                resolveDescription(product),
                product.getBarcode(),
                product.getQrCode(),
                barcodeImageUrl,
                qrImageUrl,
                descriptionGenerated,
                barcodeGenerated,
                qrGenerated,
                mirror.mirrorStatus(),
                mirror.warning()
        );
    }

    private String resolveMirrorStatus(UUID commerceProductId) {
        return productGeneratorMirrorJobRepository.findFirstByCommerceProduct_IdOrderByCreatedAtDesc(commerceProductId)
                .map(job -> job.getStatus().name())
                .orElseGet(() -> productGeneratorBridgeRepository.findByCommerceProduct_Id(commerceProductId).isPresent()
                        ? "MAPPED"
                        : "UNMAPPED"
                );
    }

    private boolean isDescriptionMissing(Product product) {
        return isMissing(resolveDescription(product));
    }

    private String resolveDescription(Product product) {
        if (StringUtils.hasText(product.getLongDescription())) {
            return product.getLongDescription().trim();
        }
        if (StringUtils.hasText(product.getShortDescription())) {
            return product.getShortDescription().trim();
        }
        return null;
    }

    private boolean isMissing(String value) {
        return value == null || value.isBlank();
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private java.util.Optional<UUID> parseUuid(String value) {
        if (!StringUtils.hasText(value)) {
            return java.util.Optional.empty();
        }
        String candidate = value.trim();
        if (candidate.length() > 2 && candidate.startsWith("{") && candidate.endsWith("}")) {
            candidate = candidate.substring(1, candidate.length() - 1).trim();
        }
        if (candidate.regionMatches(true, 0, "0x", 0, 2)) {
            candidate = candidate.substring(2);
        }
        if (HEX_32_PATTERN.matcher(candidate).matches()) {
            candidate = candidate.substring(0, 8)
                    + "-"
                    + candidate.substring(8, 12)
                    + "-"
                    + candidate.substring(12, 16)
                    + "-"
                    + candidate.substring(16, 20)
                    + "-"
                    + candidate.substring(20);
        }
        try {
            return java.util.Optional.of(UUID.fromString(candidate));
        } catch (IllegalArgumentException ex) {
            return java.util.Optional.empty();
        }
    }

    private record MirrorQueueResult(String mirrorStatus, String warning) {
    }
}
