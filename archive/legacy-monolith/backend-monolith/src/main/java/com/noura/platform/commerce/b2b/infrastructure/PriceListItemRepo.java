package com.noura.platform.commerce.b2b.infrastructure;

import com.noura.platform.commerce.b2b.domain.PriceListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriceListItemRepo extends JpaRepository<PriceListItem, Long> {

    List<PriceListItem> findByPriceListId(Long priceListId);

    @Query("SELECT pli FROM PriceListItem pli WHERE pli.priceList.id = :priceListId " +
            "AND pli.productId = :productId " +
            "AND (pli.variantId IS NULL OR pli.variantId = :variantId) " +
            "AND pli.minimumQuantity <= :quantity " +
            "ORDER BY pli.minimumQuantity DESC")
    List<PriceListItem> findApplicableItems(Long priceListId, Long productId, Long variantId, int quantity);

    Optional<PriceListItem> findByPriceListIdAndProductIdAndVariantId(Long priceListId, Long productId, Long variantId);
}
