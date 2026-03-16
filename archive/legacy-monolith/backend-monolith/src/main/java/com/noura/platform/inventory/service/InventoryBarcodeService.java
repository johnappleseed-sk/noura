package com.noura.platform.inventory.service;

public interface InventoryBarcodeService {

    byte[] generateProductBarcode(String productId, boolean qrCode, int width, int height);

    byte[] generateBatchBarcode(String batchId, boolean qrCode, int width, int height);

    byte[] generateBinBarcode(String binId, boolean qrCode, int width, int height);
}
