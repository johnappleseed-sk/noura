package com.noura.platform.inventory.domain.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ProductCategoryId implements Serializable {

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "category_id", nullable = false, length = 36)
    private String categoryId;
}
