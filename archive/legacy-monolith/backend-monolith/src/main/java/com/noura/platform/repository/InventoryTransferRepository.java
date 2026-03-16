package com.noura.platform.repository;

import com.noura.platform.domain.entity.InventoryTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryTransferRepository extends JpaRepository<InventoryTransfer, UUID> {
}
