package com.noura.platform.service.impl.productgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductCodeImageServiceTest {

    private final ProductCodeImageService service = new ProductCodeImageService();

    @Test
    void generateUniqueEan13_shouldThrowWhenAllAttemptsCollide() {
        assertThrows(IllegalStateException.class, () -> service.generateUniqueEan13(code -> true));
    }

    @Test
    void generateUniqueEan13_shouldReturnValidEan13WhenCandidateAvailable() {
        String ean = service.generateUniqueEan13(code -> false);

        assertEquals(13, ean.length());
        assertTrue(ean.chars().allMatch(Character::isDigit));
        assertEquals(computeCheckDigit(ean.substring(0, 12)), Character.digit(ean.charAt(12), 10));
    }

    @Test
    void barcodeAndQrImage_shouldRenderPngBytes() {
        byte[] barcode = service.barcodePng("1234567890128");
        byte[] qr = service.qrPng("https://store.example.com/products/123");

        assertTrue(barcode.length > 0);
        assertTrue(qr.length > 0);
    }

    private int computeCheckDigit(String first12Digits) {
        int sum = 0;
        for (int i = 0; i < first12Digits.length(); i++) {
            int digit = Character.digit(first12Digits.charAt(i), 10);
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return (10 - (sum % 10)) % 10;
    }
}
