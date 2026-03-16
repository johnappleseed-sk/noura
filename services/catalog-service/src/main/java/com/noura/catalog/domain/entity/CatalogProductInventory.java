package com.noura.catalog.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "product_inventory")
public class CatalogProductInventory {

    @Id
    private UUID id;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "store_id")
    private UUID storeId;

    private int stock;

    @Column(name = "store_price")
    private BigDecimal storePrice;

    private boolean published;

    private boolean visible;

    @Column(name = "local_name")
    private String localName;
}
