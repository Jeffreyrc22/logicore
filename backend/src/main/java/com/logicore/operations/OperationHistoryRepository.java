package com.logicore.operations;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OperationHistoryRepository
  extends
    JpaRepository<OperationHistory, Long>,
    JpaSpecificationExecutor<OperationHistory>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from OperationHistory e where e.id=:id")
  Optional<OperationHistory> lock(@Param("id") Long id);
}
