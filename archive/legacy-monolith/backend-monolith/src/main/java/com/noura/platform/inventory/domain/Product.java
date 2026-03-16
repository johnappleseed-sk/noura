package com.noura.platform.inventory.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product extends SoftDeleteEntity {

    @Column(name = "sku", nullable = false, length = 100, unique = true)
    private String sku;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "status", nullable = false, length = 40)
    private String status = "DRAFT";

    @Column(name = "base_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal basePrice;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @Column(name = "width_cm", precision = 18, scale = 4)
    private BigDecimal widthCm;

    @Column(name = "height_cm", precision = 18, scale = 4)
    private BigDecimal heightCm;

    @Column(name = "length_cm", precision = 18, scale = 4)
    private BigDecimal lengthCm;

    @Column(name = "weight_kg", precision = 18, scale = 4)
    private BigDecimal weightKg;

    @Column(name = "batch_tracked", nullable = false)
    private boolean batchTracked;

    @Column(name = "serial_tracked", nullable = false)
    private boolean serialTracked;

    @Column(name = "barcode_value", length = 255)
    private String barcodeValue;

    @Column(name = "qr_code_value", length = 255)
    private String qrCodeValue;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductCategory> productCategories = new ArrayList<>();
}
