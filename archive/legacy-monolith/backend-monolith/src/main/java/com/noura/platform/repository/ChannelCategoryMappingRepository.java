package com.noura.platform.repository;

import com.noura.platform.domain.entity.ChannelCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelCategoryMappingRepository extends JpaRepository<ChannelCategoryMapping, UUID> {
    /**
     * Finds by category id order by channel asc region code asc.
     *
     * @param categoryId The category id used to locate the target record.
     * @return A list of matching items.
     */
    List<ChannelCategoryMapping> findByCategoryIdOrderByChannelAscRegionCodeAsc(UUID categoryId);

    /**
     * Finds by category id and channel ignore case and region code ignore case.
     *
     * @param categoryId The category id used to locate the target record.
     * @param channel The channel value.
     * @param regionCode The region code value.
     * @return The matching mapping when available.
     */
    Optional<ChannelCategoryMapping> findByCategoryIdAndChannelIgnoreCaseAndRegionCodeIgnoreCase(
            UUID categoryId,
            String channel,
            String regionCode
    );
}
