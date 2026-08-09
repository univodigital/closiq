package com.closiq.inventory.repository;

import com.closiq.inventory.domain.InventoryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {
}
