package com.noura.platform.dto.product;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@ToString
public class ProductFilterRequest {
    private String query;
    private String category;
    private UUID categoryId;
    private String brand;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minRating;
    private UUID storeId;
    private Boolean availableAtStore;
    private Boolean flashSale;
    private Boolean trending;
    private String attributeKey;
    private String attributeValue;
}
