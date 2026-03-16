package com.noura.catalog.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "brands")
public class CatalogBrand {

    @Id
    private UUID id;

    private String code;

    private String name;

    private String slug;

    private boolean active;
}
