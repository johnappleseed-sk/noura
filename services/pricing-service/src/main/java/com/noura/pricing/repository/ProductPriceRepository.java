package com.noura.pricing.repository;

import com.noura.pricing.domain.entity.ProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence gateway for product price records.
 */
public interface ProductPriceRepository extends JpaRepository<ProductPrice, UUID> {

    /**
     * Retrieves all price records for a product.
     *
     * @param productId product identifier
     * @return all price records
     */
    List<ProductPrice> findByProductId(UUID productId);

    /**
     * Retrieves all price records for a product in a specific currency.
     *
     * @param productId product identifier
     * @param currencyCode currency code
     * @return matching price records
     */
    List<ProductPrice> findByProductIdAndCurrencyCodeIgnoreCase(UUID productId, String currencyCode);

    /**
     * Retrieves all price records for a product collection.
     *
     * @param productIds product identifiers
     * @return matching price records
     */
    List<ProductPrice> findByProductIdIn(Collection<UUID> productIds);

    /**
     * Retrieves all price records for products in a specific currency.
     *
     * @param productIds product identifiers
     * @param currencyCode currency code
     * @return matching price records
     */
    List<ProductPrice> findByProductIdInAndCurrencyCodeIgnoreCase(Collection<UUID> productIds, String currencyCode);

    /**
     * Retrieves a product price row by natural key.
     *
     * <p>Natural key fields are product ID, currency, scope, and window bounds.</p>
     *
     * @param productId product identifier
     * @param currencyCode currency code
     * @param storeId optional store scope
     * @param channelCode optional channel scope
     * @param startsAt optional start bound
     * @param endsAt optional end bound
     * @return existing price row when present
     */
    @Query("""
            select p from ProductPrice p
            where p.productId = :productId
              and upper(p.currencyCode) = upper(:currencyCode)
              and (
                    (:storeId is null and p.storeId is null)
                    or p.storeId = :storeId
              )
              and (
                    (:channelCode is null and p.channelCode is null)
                    or upper(p.channelCode) = upper(:channelCode)
              )
              and (
                    (:startsAt is null and p.startsAt is null)
                    or p.startsAt = :startsAt
              )
              and (
                    (:endsAt is null and p.endsAt is null)
                    or p.endsAt = :endsAt
              )
            """)
    Optional<ProductPrice> findByNaturalKey(
            @Param("productId") UUID productId,
            @Param("currencyCode") String currencyCode,
            @Param("storeId") UUID storeId,
            @Param("channelCode") String channelCode,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt
    );
}

