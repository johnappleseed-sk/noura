package com.noura.platform.commerce.marketplace.infrastructure;

import com.noura.platform.commerce.marketplace.domain.ImportStatus;
import com.noura.platform.commerce.marketplace.domain.MarketplaceOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketplaceOrderRepo extends JpaRepository<MarketplaceOrder, Long> {

    Optional<MarketplaceOrder> findByChannelIdAndExternalOrderId(Long channelId, String externalOrderId);

    boolean existsByChannelIdAndExternalOrderId(Long channelId, String externalOrderId);

    List<MarketplaceOrder> findByChannelIdOrderByCreatedAtDesc(Long channelId);

    Page<MarketplaceOrder> findByChannelId(Long channelId, Pageable pageable);

    List<MarketplaceOrder> findByImportStatus(ImportStatus status);

    List<MarketplaceOrder> findByChannelIdAndImportStatus(Long channelId, ImportStatus status);
}
