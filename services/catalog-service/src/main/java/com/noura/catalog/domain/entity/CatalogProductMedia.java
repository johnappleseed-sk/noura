package com.noura.catalog.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "product_media")
public class CatalogProductMedia {

    @Id
    private UUID id;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "media_type")
    private String mediaType;

    private String url;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "is_primary")
    private boolean primary;
}
