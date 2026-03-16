package com.noura.platform.service.impl.productgen;

import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductGeneratorBridge;
import com.noura.platform.domain.entity.ProductGeneratorMirrorJob;
import com.noura.platform.domain.enums.ProductGeneratorMirrorJobStatus;
import com.noura.platform.repository.ProductGeneratorBridgeRepository;
import com.noura.platform.repository.ProductGeneratorMirrorJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductMirrorSyncWorkerTest {

    private ProductGeneratorMirrorJobRepository mirrorJobRepository;
    private ProductGeneratorBridgeRepository bridgeRepository;
    private ProductInventoryMirrorService inventoryMirrorService;
    private ProductMirrorSyncWorker worker;

    @BeforeEach
    void setUp() {
        mirrorJobRepository = mock(ProductGeneratorMirrorJobRepository.class);
        bridgeRepository = mock(ProductGeneratorBridgeRepository.class);
        inventoryMirrorService = mock(ProductInventoryMirrorService.class);

        AppProperties appProperties = new AppProperties();
        appProperties.getProductGenerator().getMirror().setBatchSize(10);
        appProperties.getProductGenerator().getMirror().setRetryBaseSeconds(5);
        worker = new ProductMirrorSyncWorker(
                mirrorJobRepository,
                bridgeRepository,
                inventoryMirrorService,
                appProperties
        );
    }

    @Test
    void processPendingMirrorJobs_shouldBlockWhenBridgeMissing() {
        ProductGeneratorMirrorJob job = pendingJob();
        when(mirrorJobRepository.findDueJobs(any(), any(), any())).thenReturn(List.of(job));
        when(bridgeRepository.findByCommerceProduct_Id(job.getCommerceProduct().getId())).thenReturn(Optional.empty());

        worker.processPendingMirrorJobs();

        ArgumentCaptor<ProductGeneratorMirrorJob> captor = ArgumentCaptor.forClass(ProductGeneratorMirrorJob.class);
        verify(mirrorJobRepository).save(captor.capture());
        assertEquals(ProductGeneratorMirrorJobStatus.BLOCKED_MAPPING, captor.getValue().getStatus());
    }

    @Test
    void processPendingMirrorJobs_shouldMarkSyncedOnSuccessfulMirror() {
        ProductGeneratorMirrorJob job = pendingJob();
        ProductGeneratorBridge bridge = new ProductGeneratorBridge();
        bridge.setInventoryProductId(UUID.randomUUID().toString());
        when(mirrorJobRepository.findDueJobs(any(), any(), any())).thenReturn(List.of(job));
        when(bridgeRepository.findByCommerceProduct_Id(job.getCommerceProduct().getId())).thenReturn(Optional.of(bridge));

        worker.processPendingMirrorJobs();

        ArgumentCaptor<ProductGeneratorMirrorJob> captor = ArgumentCaptor.forClass(ProductGeneratorMirrorJob.class);
        verify(mirrorJobRepository).save(captor.capture());
        assertEquals(ProductGeneratorMirrorJobStatus.SYNCED, captor.getValue().getStatus());
        verify(inventoryMirrorService).mirror(
                bridge.getInventoryProductId(),
                "Generated text",
                "1234567890128",
                "https://store.example.com/products/abc"
        );
    }

    private ProductGeneratorMirrorJob pendingJob() {
        Product product = new Product();
        product.setId(UUID.randomUUID());

        ProductGeneratorMirrorJob job = new ProductGeneratorMirrorJob();
        job.setCommerceProduct(product);
        job.setStatus(ProductGeneratorMirrorJobStatus.PENDING);
        job.setPayload(Map.of(
                "description", "Generated text",
                "barcode", "1234567890128",
                "qrCode", "https://store.example.com/products/abc"
        ));
        return job;
    }
}
