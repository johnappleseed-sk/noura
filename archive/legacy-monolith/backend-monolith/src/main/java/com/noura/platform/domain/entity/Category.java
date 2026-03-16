package com.noura.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 80)
    private String code;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 255)
    private String slug;

    private String description;

    @Column(name = "classification_code", length = 120)
    private String classificationCode;

    @Column(nullable = false)
    private Integer level = 0;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private UserAccount manager;

    @Column(name = "taxonomy_version", nullable = false)
    private int taxonomyVersion = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(name = "parent_id", insertable = false, updatable = false)
    private UUID parentId;

    @OneToMany(mappedBy = "parent")
    private Set<Category> children = new LinkedHashSet<>();

    @PrePersist
    @PreUpdate
    void normalize() {
        name = trim(name);
        description = trim(description);
        classificationCode = trim(classificationCode);
        code = normalizeCode(code, "CAT");
        slug = normalizeSlug(slug, name, "category");
        level = parent == null ? 0 : parent.getLevel() + 1;
    }

    private String normalizeCode(String value, String prefix) {
        String normalized = trim(value);
        if (normalized == null) {
            return prefix + "-" + randomSuffix().toUpperCase(Locale.ROOT);
        }
        normalized = normalized.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? prefix + "-" + randomSuffix().toUpperCase(Locale.ROOT) : normalized;
    }

    private String normalizeSlug(String value, String fallback, String prefix) {
        String seed = trim(value);
        boolean generated = false;
        if (seed == null) {
            seed = fallback;
            generated = true;
        }
        if (seed == null) {
            seed = prefix;
            generated = true;
        }
        String normalized = seed.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            normalized = prefix;
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
