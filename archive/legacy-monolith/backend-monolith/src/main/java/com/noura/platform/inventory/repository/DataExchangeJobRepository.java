package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.DataExchangeJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataExchangeJobRepository extends JpaRepository<DataExchangeJob, String> {
}
