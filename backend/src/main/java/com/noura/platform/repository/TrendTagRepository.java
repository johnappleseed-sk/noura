package com.noura.platform.repository;

import com.noura.platform.domain.entity.TrendTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrendTagRepository extends JpaRepository<TrendTag, UUID> {
    /**
     * Finds top20 by order by score desc.
     *
     * @return A list of matching items.
     */
    List<TrendTag> findTop20ByOrderByScoreDesc();
}
