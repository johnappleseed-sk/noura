package com.noura.platform.inventory.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.inventory.domain.BatchLot;
import com.noura.platform.inventory.domain.Product;
import com.noura.platform.inventory.domain.WarehouseBin;
import com.noura.platform.inventory.repository.BatchLotRepository;
import com.noura.platform.inventory.repository.InventoryProductRepository;
import com.noura.platform.inventory.repository.WarehouseBinRepository;
import com.noura.platform.inventory.service.InventoryBarcodeService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class InventoryBarcodeServiceImpl implements InventoryBarcodeService {

    private final InventoryProductRepository productRepository;
    private final BatchLotRepository batchLotRepository;
    private final WarehouseBinRepository warehouseBinRepository;
    private final MultiFormatWriter multiFormatWriter = new MultiFormatWriter();

    public InventoryBarcodeServiceImpl(InventoryProductRepository productRepository,
                                       BatchLotRepository batchLotRepository,
                                       WarehouseBinRepository warehouseBinRepository) {
        this.productRepository = productRepository;
        this.batchLotRepository = batchLotRepository;
        this.warehouseBinRepository = warehouseBinRepository;
    }

    @Override
    public byte[] generateProductBarcode(String productId, boolean qrCode, int width, int height) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
        String content = product.getBarcodeValue() != null && !product.getBarcodeValue().isBlank()
                ? product.getBarcodeValue()
                : product.getSku();
        return render(content, qrCode, width, height);
    }

    @Override
    public byte[] generateBatchBarcode(String batchId, boolean qrCode, int width, int height) {
        BatchLot batch = batchLotRepository.findByIdAndDeletedAtIsNull(batchId)
                .orElseThrow(() -> new NotFoundException("BATCH_NOT_FOUND", "Batch not found"));
        return render(batch.getLotNumber(), qrCode, width, height);
    }

    @Override
    public byte[] generateBinBarcode(String binId, boolean qrCode, int width, int height) {
        WarehouseBin bin = warehouseBinRepository.findByIdAndDeletedAtIsNull(binId)
                .orElseThrow(() -> new NotFoundException("BIN_NOT_FOUND", "Warehouse bin not found"));
        String content = bin.getBarcodeValue() != null && !bin.getBarcodeValue().isBlank()
                ? bin.getBarcodeValue()
                : bin.getBinCode();
        return render(content, qrCode, width, height);
    }

    private byte[] render(String content, boolean qrCode, int width, int height) {
        try {
            BitMatrix matrix = multiFormatWriter.encode(
                    content,
                    qrCode ? BarcodeFormat.QR_CODE : BarcodeFormat.CODE_128,
                    Math.max(width, qrCode ? 240 : 360),
                    Math.max(height, qrCode ? 240 : 120)
            );
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate barcode image", ex);
        }
    }
}
