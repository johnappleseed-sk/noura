package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.SerialNumber;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SerialNumberRepository extends JpaRepository<SerialNumber, String>, JpaSpecificationExecutor<SerialNumber> {

    Optional<SerialNumber> findByIdAndDeletedAtIsNull(String serialId);

    Optional<SerialNumber> findBySerialNumberAndDeletedAtIsNull(String serialNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select serial from SerialNumber serial
            where serial.product.id = :productId
              and (:warehouseId is null or serial.warehouse.id = :warehouseId)
              and (:binId is null or serial.bin.id = :binId)
              and (:batchId is null or serial.batch.id = :batchId)
              and serial.deletedAt is null
              and serial.serialStatus = 'IN_STOCK'
            order by serial.createdAt asc
            """)
    List<SerialNumber> findAvailableForUpdate(@Param("productId") String productId,
                                              @Param("warehouseId") String warehouseId,
                                              @Param("binId") String binId,
                                              @Param("batchId") String batchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select serial from SerialNumber serial
            where serial.product.id = :productId
              and serial.serialNumber in :serialNumbers
              and serial.deletedAt is null
            order by serial.createdAt asc
            """)
    List<SerialNumber> findByProductAndSerialsForUpdate(@Param("productId") String productId,
                                                        @Param("serialNumbers") List<String> serialNumbers);
}
