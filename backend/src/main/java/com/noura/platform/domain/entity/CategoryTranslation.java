package com.noura.platform.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "category_translations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_category_translation_category_locale", columnNames = {"category_id", "locale"})
        }
)
public class CategoryTranslation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(name = "localized_name", nullable = false)
    private String localizedName;

    @Column(name = "localized_description")
    private String localizedDescription;

    @Column(name = "seo_slug")
    private String seoSlug;
}
