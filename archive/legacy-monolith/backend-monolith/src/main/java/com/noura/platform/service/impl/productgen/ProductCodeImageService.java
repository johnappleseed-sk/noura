package com.noura.platform.service.impl.productgen;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.function.Predicate;

@Component
public class ProductCodeImageService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final MultiFormatWriter writer = new MultiFormatWriter();

    public String generateUniqueEan13(Predicate<String> barcodeExists) {
        for (int attempts = 0; attempts < 20; attempts++) {
            String body = randomDigits(12);
            String barcode = body + calculateCheckDigit(body);
            if (!barcodeExists.test(barcode)) {
                return barcode;
            }
        }
        throw new IllegalStateException("Unable to generate a unique barcode.");
    }

    public byte[] barcodePng(String barcode) {
        return render(barcode, BarcodeFormat.EAN_13, 420, 140);
    }

    public byte[] qrPng(String payload) {
        return render(payload, BarcodeFormat.QR_CODE, 280, 280);
    }

    private byte[] render(String content, BarcodeFormat format, int width, int height) {
        try {
            BitMatrix matrix = writer.encode(content, format, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to render code image.", ex);
        }
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private int calculateCheckDigit(String first12Digits) {
        int sum = 0;
        for (int i = 0; i < first12Digits.length(); i++) {
            int digit = Character.digit(first12Digits.charAt(i), 10);
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return (10 - (sum % 10)) % 10;
    }
}
