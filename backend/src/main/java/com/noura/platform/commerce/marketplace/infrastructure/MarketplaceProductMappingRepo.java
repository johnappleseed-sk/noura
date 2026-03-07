package com.noura.platform.commerce.marketplace.infrastructure;

import com.noura.platform.commerce.marketplace.domain.MarketplaceProductMapping;
import com.noura.platform.commerce.marketplace.domain.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketplaceProductMappingRepo extends JpaRepository<MarketplaceProductMapping, Long> {

    List<MarketplaceProductMapping> findByChannelId(Long channelId);

    List<MarketplaceProductMapping> findByProductId(Long productId);

    Optional<MarketplaceProductMapping> findByChannelIdAndProductIdAndVariantId(
            Long channelId, Long productId, Long variantId);

    Optional<MarketplaceProductMapping> findByChannelIdAndExternalProductId(
            Long channelId, String externalProductId);

    List<MarketplaceProductMapping> findByChannelIdAndStatus(Long channelId, ListingStatus status);
}
