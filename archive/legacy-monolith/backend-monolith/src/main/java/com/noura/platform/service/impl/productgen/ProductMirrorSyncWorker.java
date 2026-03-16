package com.noura.platform.service.impl.productgen;

import com.noura.platform.config.AppProperties;
import com.noura.platform.domain.entity.ProductGeneratorBridge;
import com.noura.platform.domain.entity.ProductGeneratorMirrorJob;
import com.noura.platform.domain.enums.ProductGeneratorMirrorJobStatus;
import com.noura.platform.repository.ProductGeneratorBridgeRepository;
import com.noura.platform.repository.ProductGeneratorMirrorJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMirrorSyncWorker {

    private final ProductGeneratorMirrorJobRepository productGeneratorMirrorJobRepository;
    private final ProductGeneratorBridgeRepository productGeneratorBridgeRepository;
    private final ProductInventoryMirrorService productInventoryMirrorService;
    private final AppProperties appProperties;

    @Scheduled(fixedDelayString = "${app.product-generator.mirror.worker-delay-ms:30000}")
    @Transactional
    public void processPendingMirrorJobs() {
        Instant now = Instant.now();
        int batchSize = Math.max(1, appProperties.getProductGenerator().getMirror().getBatchSize());

        List<ProductGeneratorMirrorJob> jobs = productGeneratorMirrorJobRepository.findDueJobs(
                List.of(ProductGeneratorMirrorJobStatus.PENDING, ProductGeneratorMirrorJobStatus.RETRYING),
                now,
                PageRequest.of(0, batchSize)
        );

        for (ProductGeneratorMirrorJob job : jobs) {
            processJob(job, now);
        }
    }

    private void processJob(ProductGeneratorMirrorJob job, Instant now) {
        ProductGeneratorBridge bridge = productGeneratorBridgeRepository.findByCommerceProduct_Id(job.getCommerceProduct().getId())
                .orElse(null);
        if (bridge == null) {
            job.setStatus(ProductGeneratorMirrorJobStatus.BLOCKED_MAPPING);
            job.setLastError("No inventory mapping found for commerce product.");
            job.setNextRetryAt(null);
            productGeneratorMirrorJobRepository.save(job);
            return;
        }

        Map<String, Object> payload = job.getPayload() == null ? Map.of() : job.getPayload();
        String description = asString(payload.get("description"));
        String barcode = asString(payload.get("barcode"));
        String qrCode = asString(payload.get("qrCode"));

        try {
            productInventoryMirrorService.mirror(bridge.getInventoryProductId(), description, barcode, qrCode);
            job.setStatus(ProductGeneratorMirrorJobStatus.SYNCED);
            job.setAttempts(job.getAttempts() + 1);
            job.setLastError(null);
            job.setNextRetryAt(null);
            productGeneratorMirrorJobRepository.save(job);
        } catch (Exception ex) {
            int attempts = job.getAttempts() + 1;
            job.setAttempts(attempts);
            job.setLastError(ex.getMessage());

            if (attempts >= Math.max(1, job.getMaxAttempts())) {
                job.setStatus(ProductGeneratorMirrorJobStatus.FAILED);
                job.setNextRetryAt(null);
            } else {
                job.setStatus(ProductGeneratorMirrorJobStatus.RETRYING);
                long baseSeconds = Math.max(5, appProperties.getProductGenerator().getMirror().getRetryBaseSeconds());
                long backoff = (long) Math.pow(2, Math.max(0, attempts - 1));
                job.setNextRetryAt(now.plusSeconds(baseSeconds * backoff));
            }

            productGeneratorMirrorJobRepository.save(job);
            log.warn("Failed to mirror product generator job {} (attempt {}): {}", job.getId(), attempts, ex.getMessage());
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
