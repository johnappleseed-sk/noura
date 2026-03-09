package com.noura.platform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "warehouses")
public class Warehouse extends SoftDeleteEntity {

    @Column(name = "warehouse_code", nullable = false, length = 80, unique = true)
    private String warehouseCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "warehouse_type", nullable = false, length = 60)
    private String warehouseType = "FULFILLMENT";

    @Column(name = "address_line_1", length = 255)
    private String addressLine1;

    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "state_province", length = 120)
    private String stateProvince;

    @Column(name = "postal_code", length = 40)
    private String postalCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
