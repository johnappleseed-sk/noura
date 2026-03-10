package com.noura.platform.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.noura.platform.dto.product.ProductGeneratorRequest;
import com.noura.platform.dto.product.ProductGeneratorResponse;
import com.noura.platform.service.ProductGeneratorService;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

@Service
public class ProductGeneratorServiceImpl implements ProductGeneratorService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String SALT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String SYMBOL_CHARS = "!@#$%&*";
    private static final String MIX_CHARS = SALT_CHARS + SYMBOL_CHARS;
    private static final int QR_SIZE = 300;

    @Override
    public ProductGeneratorResponse generate(ProductGeneratorRequest request) {
        String productName = resolveProductName(request);
        String description = buildDescription(productName, request);
        String barcode = generateBarcode();
        String qrBase64 = generateQrCodeBase64(barcode);

        return ProductGeneratorResponse.builder()
                .productName(productName)
                .description(description)
                .barcode(barcode)
                .qrCodeBase64(qrBase64)
                .build();
    }

    private String resolveProductName(ProductGeneratorRequest request) {
        if (request.getName() != null && !request.getName().isBlank()) {
            return request.getName().trim();
        }
        String brand = request.getBrand() != null ? request.getBrand().trim() : "Noura";
        String category = request.getCategory() != null ? request.getCategory().trim() : "Product";
        return brand + " " + category + " " + randomAlphanumeric(4).toUpperCase();
    }

    private String buildDescription(String name, ProductGeneratorRequest request) {
        String category = request.getCategory() != null ? request.getCategory().trim() : "general merchandise";
        String brand = request.getBrand() != null ? request.getBrand().trim() : "Noura";
        String audience = request.getTargetAudience() != null ? request.getTargetAudience().trim() : "discerning consumers";

        return String.join("\n\n",
                "PRODUCT NAME: " + name,

                "OVERVIEW: " +
                        "Introducing " + name + " — a premium " + category + " engineered by " + brand +
                        " for " + audience + ". Built with meticulous attention to detail, this product " +
                        "combines cutting-edge innovation with enterprise-grade reliability to deliver " +
                        "an unparalleled experience.",

                "KEY FEATURES:\n" +
                        "• Premium build quality with materials sourced from certified global suppliers\n" +
                        "• Advanced performance engineering optimized for daily professional use\n" +
                        "• Ergonomic design tested across diverse user groups for maximum comfort\n" +
                        "• Seamless integration with existing " + brand + " ecosystem products\n" +
                        "• Industry-leading energy efficiency and sustainability certification",

                "BENEFITS:\n" +
                        "• Increases operational productivity and reduces total cost of ownership\n" +
                        "• Backed by " + brand + "'s comprehensive warranty and 24/7 support network\n" +
                        "• Designed for scalability — adapts to evolving business requirements\n" +
                        "• Reduces environmental footprint with eco-certified manufacturing processes",

                "SPECIFICATIONS:\n" +
                        "• Category: " + capitalize(category) + "\n" +
                        "• Brand: " + brand + "\n" +
                        "• Target audience: " + capitalize(audience) + "\n" +
                        "• Compliance: ISO 9001, CE, RoHS\n" +
                        "• Warranty: 24-month standard coverage\n" +
                        "• Origin: Certified global supply chain",

                "MARKETING HIGHLIGHTS:\n" +
                        "• \"The future of " + category.toLowerCase() + " starts here — " + name + " by " + brand + ".\"\n" +
                        "• Featured in enterprise procurement catalogs worldwide\n" +
                        "• Eligible for volume pricing and B2B partnership agreements\n" +
                        "• Ready for omni-channel retail and e-commerce integration"
        );
    }

    /**
     * Format: NOURA + YYYYMMDDHHMMSS + salt(5) + engrp + mix(6)
     * Example: NOURA20260310143045Xy9Aaengrpa7B#1x
     */
    private String generateBarcode() {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String salt = randomFrom(SALT_CHARS, 5);
        String mix = randomMix(6);
        return "NOURA" + timestamp + salt + "engrp" + mix;
    }

    private String generateQrCodeBase64(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.MARGIN, 1
            );
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    private String randomFrom(String chars, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String randomMix(int length) {
        StringBuilder sb = new StringBuilder(length);
        // Guarantee at least one letter, one digit, and one symbol
        sb.append(SALT_CHARS.charAt(RANDOM.nextInt(52))); // letter
        sb.append(SALT_CHARS.charAt(52 + RANDOM.nextInt(10))); // digit
        sb.append(SYMBOL_CHARS.charAt(RANDOM.nextInt(SYMBOL_CHARS.length()))); // symbol
        for (int i = 3; i < length; i++) {
            sb.append(MIX_CHARS.charAt(RANDOM.nextInt(MIX_CHARS.length())));
        }
        // Shuffle to avoid positional predictability
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return new String(arr);
    }

    private String randomAlphanumeric(int length) {
        return randomFrom(SALT_CHARS, length);
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
