package com.logicore.operations;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OperationRepository
  extends JpaRepository<Operation, Long>, JpaSpecificationExecutor<Operation>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Operation e where e.id=:id")
  Optional<Operation> lock(@Param("id") Long id);
}
