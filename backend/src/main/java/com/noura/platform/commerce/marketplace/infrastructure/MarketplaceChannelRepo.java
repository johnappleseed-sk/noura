package com.noura.platform.commerce.marketplace.infrastructure;

import com.noura.platform.commerce.marketplace.domain.MarketplaceChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketplaceChannelRepo extends JpaRepository<MarketplaceChannel, Long> {

    Optional<MarketplaceChannel> findByCode(String code);

    List<MarketplaceChannel> findByActiveTrue();

    boolean existsByCode(String code);
}
