package com.noura.platform.commerce.b2b.infrastructure;

import com.noura.platform.commerce.b2b.domain.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriceListRepo extends JpaRepository<PriceList, Long> {

    Optional<PriceList> findByCode(String code);

    List<PriceList> findByActiveTrueOrderByPriority();

    List<PriceList> findByActiveTrue();

    @Query("SELECT pl FROM PriceList pl WHERE pl.active = true " +
            "AND (pl.validFrom IS NULL OR pl.validFrom <= CURRENT_DATE) " +
            "AND (pl.validUntil IS NULL OR pl.validUntil >= CURRENT_DATE) " +
            "ORDER BY pl.priority")
    List<PriceList> findValidPriceLists();
}
