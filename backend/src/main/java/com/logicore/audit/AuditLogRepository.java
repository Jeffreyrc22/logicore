package com.logicore.audit;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository
  extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from AuditLog e where e.id=:id")
  Optional<AuditLog> lock(@Param("id") Long id);
}
