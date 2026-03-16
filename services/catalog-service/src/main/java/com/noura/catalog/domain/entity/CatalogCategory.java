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
@Table(name = "categories")
public class CatalogCategory {

    @Id
    private UUID id;

    private String code;

    private String name;

    private String slug;

    private String description;

    @Column(name = "classification_code")
    private String classificationCode;

    private Integer level;

    private boolean active;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "parent_id")
    private UUID parentId;
}
