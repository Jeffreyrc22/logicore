package com.logicore.inventory;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InventoryMovementRepository
  extends
    JpaRepository<InventoryMovement, Long>,
    JpaSpecificationExecutor<InventoryMovement>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from InventoryMovement e where e.id=:id")
  Optional<InventoryMovement> lock(@Param("id") Long id);
}
