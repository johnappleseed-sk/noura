package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.ProductCategory;
import com.noura.platform.inventory.domain.id.ProductCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, ProductCategoryId> {

    boolean existsByCategory_IdAndProduct_DeletedAtIsNull(String categoryId);
}
