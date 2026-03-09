package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.StockMovementLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.Instant;

public interface StockMovementLineRepository extends JpaRepository<StockMovementLine, String> {

    @Query("""
            select line from StockMovementLine line
            left join fetch line.product product
            left join fetch line.batch batch
            left join fetch line.fromBin fromBin
            left join fetch line.toBin toBin
            where line.movement.id = :movementId
            order by line.lineNumber asc
            """)
    List<StockMovementLine> findAllDetailedByMovementId(@Param("movementId") String movementId);

    @Query("""
            select line.product.id, line.product.sku, line.product.name, coalesce(sum(line.quantity), 0)
            from StockMovementLine line
            join line.movement movement
            where movement.deletedAt is null
              and movement.movementStatus = 'COMPLETED'
              and movement.movementType = 'OUTBOUND'
              and movement.processedAt >= :processedFrom
              and movement.processedAt <= :processedTo
            group by line.product.id, line.product.sku, line.product.name
            order by line.product.sku asc
            """)
    List<Object[]> summarizeOutboundByProduct(@Param("processedFrom") Instant processedFrom,
                                              @Param("processedTo") Instant processedTo);
}
