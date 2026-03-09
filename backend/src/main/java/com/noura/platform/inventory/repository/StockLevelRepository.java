package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.StockLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, String>, JpaSpecificationExecutor<StockLevel> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select sl from StockLevel sl
            left join fetch sl.batch batch
            left join fetch sl.bin bin
            where sl.product.id = :productId
              and sl.warehouse.id = :warehouseId
              and ((:binId is null and sl.bin is null) or bin.id = :binId)
              and ((:batchId is null and sl.batch is null) or batch.id = :batchId)
            """)
    Optional<StockLevel> findForUpdate(@Param("productId") String productId,
                                       @Param("warehouseId") String warehouseId,
                                       @Param("binId") String binId,
                                       @Param("batchId") String batchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select sl from StockLevel sl
            left join fetch sl.batch batch
            left join fetch sl.bin bin
            where sl.product.id = :productId
              and sl.warehouse.id = :warehouseId
              and (:binId is null or bin.id = :binId)
              and (:batchId is null or batch.id = :batchId)
              and sl.quantityAvailable > 0
            order by
              case when batch.expiryDate is null then 1 else 0 end asc,
              batch.expiryDate asc,
              batch.receivedAt asc,
              sl.createdAt asc
            """)
    List<StockLevel> findAllocatableLevels(@Param("productId") String productId,
                                           @Param("warehouseId") String warehouseId,
                                           @Param("binId") String binId,
                                           @Param("batchId") String batchId);

    @Query("""
            select coalesce(sum(sl.quantityAvailable), 0) from StockLevel sl
            where sl.product.id = :productId
              and sl.warehouse.id = :warehouseId
            """)
    BigDecimal sumAvailableByProductAndWarehouse(@Param("productId") String productId,
                                                 @Param("warehouseId") String warehouseId);

    @Query("""
            select coalesce(sum(sl.quantityAvailable), 0) from StockLevel sl
            where sl.product.id = :productId
            """)
    BigDecimal sumAvailableByProduct(@Param("productId") String productId);

    @Query("""
            select coalesce(sum(sl.quantityOnHand), 0) from StockLevel sl
            where sl.batch.id = :batchId
            """)
    BigDecimal sumOnHandByBatch(@Param("batchId") String batchId);

    @Query("""
            select coalesce(sum(sl.quantityAvailable), 0) from StockLevel sl
            where sl.batch.id = :batchId
            """)
    BigDecimal sumAvailableByBatch(@Param("batchId") String batchId);
}
