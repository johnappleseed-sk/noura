package com.noura.platform.inventory.api;

import com.noura.platform.inventory.service.InventoryBarcodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1/barcodes")
public class InventoryBarcodeController {

    private final InventoryBarcodeService inventoryBarcodeService;

    @GetMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ResponseEntity<byte[]> productBarcode(@PathVariable String productId,
                                                 @RequestParam(defaultValue = "false") boolean qr,
                                                 @RequestParam(defaultValue = "360") int width,
                                                 @RequestParam(defaultValue = "120") int height) {
        return png(inventoryBarcodeService.generateProductBarcode(productId, qr, width, height), "product-" + productId + ".png");
    }

    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ResponseEntity<byte[]> batchBarcode(@PathVariable String batchId,
                                               @RequestParam(defaultValue = "false") boolean qr,
                                               @RequestParam(defaultValue = "360") int width,
                                               @RequestParam(defaultValue = "120") int height) {
        return png(inventoryBarcodeService.generateBatchBarcode(batchId, qr, width, height), "batch-" + batchId + ".png");
    }

    @GetMapping("/bins/{binId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')")
    public ResponseEntity<byte[]> binBarcode(@PathVariable String binId,
                                             @RequestParam(defaultValue = "false") boolean qr,
                                             @RequestParam(defaultValue = "360") int width,
                                             @RequestParam(defaultValue = "120") int height) {
        return png(inventoryBarcodeService.generateBinBarcode(binId, qr, width, height), "bin-" + binId + ".png");
    }

    private ResponseEntity<byte[]> png(byte[] content, String filename) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename).build().toString())
                .body(content);
    }
}
