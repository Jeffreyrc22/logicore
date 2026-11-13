package com.logicore.inventory;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InventoryItemRepository
  extends
    JpaRepository<InventoryItem, Long>,
    JpaSpecificationExecutor<InventoryItem>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from InventoryItem e where e.id=:id")
  Optional<InventoryItem> lock(@Param("id") Long id);
}
