package com.mesproject.repo;

import com.mesproject.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
}
