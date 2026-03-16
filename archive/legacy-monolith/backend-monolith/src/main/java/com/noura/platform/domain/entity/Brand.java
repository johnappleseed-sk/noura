package com.noura.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "brands")
public class Brand extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 80)
    private String code;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 255)
    private String slug;

    @Column(nullable = false)
    private boolean active = true;

    @PrePersist
    @PreUpdate
    void normalize() {
        name = trim(name);
        code = normalizeCode(code);
        slug = normalizeSlug(slug, name);
    }

    private String normalizeCode(String value) {
        String normalized = trim(value);
        if (normalized == null) {
            return "BRD-" + randomSuffix().toUpperCase(Locale.ROOT);
        }
        normalized = normalized.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "BRD-" + randomSuffix().toUpperCase(Locale.ROOT) : normalized;
    }

    private String normalizeSlug(String value, String fallback) {
        String seed = trim(value);
        boolean generated = false;
        if (seed == null) {
            seed = fallback;
            generated = true;
        }
        if (seed == null) {
            seed = "brand";
            generated = true;
        }
        String normalized = seed.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            normalized = "brand";
            generated = true;
        }
        if (generated) {
            return normalized + "-" + randomSuffix().toLowerCase(Locale.ROOT);
        }
        return normalized;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
