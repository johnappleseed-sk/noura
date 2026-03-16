package com.noura.platform.service.impl;

import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductSubmission;
import com.noura.platform.repository.ProductRepository;
import com.noura.platform.service.ProductDeduplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductDeduplicationServiceImpl implements ProductDeduplicationService {

    private final ProductRepository productRepository;

    @Override
    public String buildSimilarityHash(String proposedName, String proposedBarcode, String proposedBrand) {
        String seed = normalizeName(proposedName)
                + "|"
                + normalizeBarcode(proposedBarcode)
                + "|"
                + normalizeName(proposedBrand);
        return sha256Hex(seed);
    }

    @Override
    public Optional<Product> findPotentialMatch(ProductSubmission submission) {
        if (submission == null) {
            return Optional.empty();
        }

        String barcode = normalizeBarcode(submission.getProposedBarcode());
        if (!barcode.isBlank()) {
            Optional<Product> barcodeMatch = productRepository.findByBarcodeIgnoreCase(barcode)
                    .filter(product -> brandMatches(product, submission.getProposedBrand()) || nameMatches(product, submission.getProposedName()));
            if (barcodeMatch.isPresent()) {
                return barcodeMatch;
            }
        }

        String normalizedName = normalizeName(submission.getProposedName());
        if (!normalizedName.isBlank()) {
            return productRepository.findTop20ByActiveTrueAndNormalizedNameContainingIgnoreCaseOrderByUpdatedAtDesc(normalizedName).stream()
                    .filter(product -> brandMatches(product, submission.getProposedBrand()))
                    .findFirst();
        }

        return Optional.empty();
    }

    private boolean brandMatches(Product product, String proposedBrand) {
        String candidateBrand = product.getBrand() == null ? "" : normalizeName(product.getBrand().getName());
        String requestedBrand = normalizeName(proposedBrand);
        return requestedBrand.isBlank() || candidateBrand.equals(requestedBrand);
    }

    private boolean nameMatches(Product product, String proposedName) {
        return normalizeName(product.getName()).equals(normalizeName(proposedName));
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeBarcode(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte current : hash) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }
}
