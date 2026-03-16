package com.noura.search.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Immutable
@Entity
@Table(name = "products")
public class SearchProduct {

    @Id
    private UUID id;

    @Column(name = "product_code")
    private String productCode;

    private String name;

    @Column(name = "category_id")
    private UUID categoryId;

    private boolean active;

    private boolean trending;

    @Column(name = "popularity_score")
    private int popularityScore;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
