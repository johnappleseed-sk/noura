package com.noura.platform.repository;

import com.noura.platform.domain.entity.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceRepository extends JpaRepository<Price, UUID> {
    /**
     * Finds by variant id.
     *
     * @param variantId The variant id used to locate the target record.
     * @return A list of matching items.
     */
    List<Price> findByVariantId(UUID variantId);

    /**
     * Finds by natural key.
     *
     * @param variantId The variant id used to locate the target record.
     * @param priceListId The price list id used to locate the target record.
     * @param currency The currency value.
     * @param startDate The start date value.
     * @param endDate The end date value.
     * @return The matching entry when available.
     */
    @Query("""
            select p from Price p
            where p.variant.id = :variantId
              and p.priceList.id = :priceListId
              and upper(p.currency) = upper(:currency)
              and (
                    (:startDate is null and p.startDate is null)
                    or p.startDate = :startDate
              )
              and (
                    (:endDate is null and p.endDate is null)
                    or p.endDate = :endDate
              )
            """)
    Optional<Price> findByNaturalKey(
            @Param("variantId") UUID variantId,
            @Param("priceListId") UUID priceListId,
            @Param("currency") String currency,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}
