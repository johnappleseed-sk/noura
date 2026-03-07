package com.noura.platform.mapper;

import com.noura.platform.domain.entity.Product;
import com.noura.platform.domain.entity.ProductMedia;
import com.noura.platform.domain.entity.ProductReview;
import com.noura.platform.domain.entity.ProductVariant;
import com.noura.platform.dto.product.ProductDto;
import com.noura.platform.dto.product.ProductMediaDto;
import com.noura.platform.dto.product.ProductReviewDto;
import com.noura.platform.dto.product.ProductVariantDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    /**
     * Maps source data to ProductDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    @Mapping(target = "category", source = "category.name")
    @Mapping(target = "brand", source = "brand.name")
    @Mapping(target = "price", source = "basePrice")
    @Mapping(target = "seo", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "media", ignore = true)
    @Mapping(target = "storeInventory", ignore = true)
    ProductDto toDto(Product entity);

    /**
     * Maps source data to ProductVariantDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    ProductVariantDto toVariantDto(ProductVariant entity);

    /**
     * Maps source data to ProductMediaDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    ProductMediaDto toMediaDto(ProductMedia entity);

    /**
     * Maps source data to ProductReviewDto.
     *
     * @param entity The source object to transform.
     * @return The mapped DTO representation.
     */
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.fullName")
    ProductReviewDto toReviewDto(ProductReview entity);
}
