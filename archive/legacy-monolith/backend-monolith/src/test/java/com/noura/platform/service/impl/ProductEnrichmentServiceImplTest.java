package com.noura.platform.service.impl;

import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.entity.Category;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductGeneratorBridge;
import com.noura.platform.domain.entity.ProductGeneratorMirrorJob;
import com.noura.platform.domain.enums.ProductGeneratorMirrorJobStatus;
import com.noura.platform.repository.ProductGeneratorBridgeRepository;
import com.noura.platform.repository.ProductGeneratorMirrorJobRepository;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.repository.ProductVariantRepository;
import com.noura.platform.service.impl.productgen.ProductCodeImageService;
import com.noura.platform.service.impl.productgen.ProductDescriptionGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductEnrichmentServiceImplTest {

    private ProductRepository productRepository;
    private ProductVariantRepository productVariantRepository;
    private ProductGeneratorBridgeRepository bridgeRepository;
    private ProductGeneratorMirrorJobRepository mirrorJobRepository;
    private ProductDescriptionGenerationService descriptionGenerationService;
    private ProductCodeImageService codeImageService;
    private AppProperties appProperties;
    private ProductEnrichmentServiceImpl service;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        productVariantRepository = mock(ProductVariantRepository.class);
        bridgeRepository = mock(ProductGeneratorBridgeRepository.class);
        mirrorJobRepository = mock(ProductGeneratorMirrorJobRepository.class);
        descriptionGenerationService = mock(ProductDescriptionGenerationService.class);
        codeImageService = mock(ProductCodeImageService.class);
        appProperties = new AppProperties();
        service = new ProductEnrichmentServiceImpl(
                productRepository,
                productVariantRepository,
                bridgeRepository,
                mirrorJobRepository,
                descriptionGenerationService,
                codeImageService,
                appProperties
        );
    }

    @Test
    void generateMissingFields_shouldGenerateMissingValuesAndBlockMirrorWithoutMapping() {
        Product product = activeProduct("Noura Phone");
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(descriptionGenerationService.generate(any())).thenReturn("Generated product description text.");
        when(codeImageService.generateUniqueEan13(any())).thenReturn("1234567890128");
        when(bridgeRepository.findByCommerceProduct_Id(product.getId())).thenReturn(Optional.empty());

        var response = service.generateMissingFields(product.getId());

        assertTrue(response.descriptionGenerated());
        assertTrue(response.barcodeGenerated());
        assertTrue(response.qrGenerated());
        assertEquals(ProductGeneratorMirrorJobStatus.BLOCKED_MAPPING.name(), response.mirrorStatus());
        verify(productRepository).save(product);

        ArgumentCaptor<ProductGeneratorMirrorJob> jobCaptor = ArgumentCaptor.forClass(ProductGeneratorMirrorJob.class);
        verify(mirrorJobRepository).save(jobCaptor.capture());
        assertEquals(ProductGeneratorMirrorJobStatus.BLOCKED_MAPPING, jobCaptor.getValue().getStatus());
    }

    @Test
    void generateMissingFields_shouldSkipGenerationWhenValuesAlreadyPresent() {
        Product product = activeProduct("Noura Headset");
        product.setLongDescription("Existing description");
        product.setBarcode("1234567890128");
        product.setQrCode("https://store.example.com/products/" + product.getId());

        ProductGeneratorBridge bridge = new ProductGeneratorBridge();
        bridge.setCommerceProduct(product);
        bridge.setInventoryProductId(UUID.randomUUID().toString());

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(bridgeRepository.findByCommerceProduct_Id(product.getId())).thenReturn(Optional.of(bridge));
        when(mirrorJobRepository.findFirstByCommerceProduct_IdOrderByCreatedAtDesc(product.getId())).thenReturn(Optional.empty());

        var response = service.generateMissingFields(product.getId());

        assertFalse(response.descriptionGenerated());
        assertFalse(response.barcodeGenerated());
        assertFalse(response.qrGenerated());
        assertEquals("MAPPED", response.mirrorStatus());
        verify(productRepository, never()).save(any(Product.class));
        verify(mirrorJobRepository, never()).save(any(ProductGeneratorMirrorJob.class));
    }

    @Test
    void searchExistingProducts_shouldReturnMirrorStatusFromLatestJob() {
        Product product = activeProduct("Noura Tablet");
        when(productRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc("tablet"))
                .thenReturn(java.util.List.of(product));
        when(productVariantRepository.findTop20BySkuContainingIgnoreCaseOrderByUpdatedAtDesc("tablet"))
                .thenReturn(java.util.List.of());

        ProductGeneratorMirrorJob job = new ProductGeneratorMirrorJob();
        job.setStatus(ProductGeneratorMirrorJobStatus.SYNCED);
        job.setCreatedAt(Instant.now());
        when(mirrorJobRepository.findFirstByCommerceProduct_IdOrderByCreatedAtDesc(product.getId()))
                .thenReturn(Optional.of(job));

        var results = service.searchExistingProducts("tablet");

        assertEquals(1, results.size());
        assertEquals(ProductGeneratorMirrorJobStatus.SYNCED.name(), results.getFirst().mirrorStatus());
    }

    @Test
    void searchExistingProducts_shouldMatchUuidProvidedAs0xHex() {
        Product product = activeProduct("Noura Speaker");
        String compactUpper = product.getId().toString().replace("-", "").toUpperCase();
        String query = "0x" + compactUpper;

        when(productRepository.findByIdAndActiveTrue(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(query))
                .thenReturn(java.util.List.of());
        when(productVariantRepository.findTop20BySkuContainingIgnoreCaseOrderByUpdatedAtDesc(query))
                .thenReturn(java.util.List.of());
        when(mirrorJobRepository.findFirstByCommerceProduct_IdOrderByCreatedAtDesc(product.getId()))
                .thenReturn(Optional.empty());
        when(bridgeRepository.findByCommerceProduct_Id(product.getId()))
                .thenReturn(Optional.empty());

        var results = service.searchExistingProducts(query);

        assertEquals(1, results.size());
        assertEquals(product.getId(), results.getFirst().id());
    }

    @Test
    void searchExistingProducts_shouldMatchUuidProvidedAsCompactHex() {
        Product product = activeProduct("Noura Console");
        String query = product.getId().toString().replace("-", "");

        when(productRepository.findByIdAndActiveTrue(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(query))
                .thenReturn(java.util.List.of());
        when(productVariantRepository.findTop20BySkuContainingIgnoreCaseOrderByUpdatedAtDesc(query))
                .thenReturn(java.util.List.of());
        when(mirrorJobRepository.findFirstByCommerceProduct_IdOrderByCreatedAtDesc(product.getId()))
                .thenReturn(Optional.empty());
        when(bridgeRepository.findByCommerceProduct_Id(product.getId()))
                .thenReturn(Optional.empty());

        var results = service.searchExistingProducts(query);

        assertEquals(1, results.size());
        assertEquals(product.getId(), results.getFirst().id());
    }

    private Product activeProduct(String name) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setActive(true);
        Category category = new Category();
        category.setName("Electronics");
        product.setCategory(category);
        return product;
    }
}
