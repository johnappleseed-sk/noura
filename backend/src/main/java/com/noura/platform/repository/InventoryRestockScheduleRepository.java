package com.noura.platform.repository;

import com.noura.platform.domain.entity.InventoryRestockSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryRestockScheduleRepository extends JpaRepository<InventoryRestockSchedule, UUID> {
}
