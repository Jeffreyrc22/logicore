package com.logicore.documents;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DocumentRecordRepository
  extends
    JpaRepository<DocumentRecord, Long>,
    JpaSpecificationExecutor<DocumentRecord>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from DocumentRecord e where e.id=:id")
  Optional<DocumentRecord> lock(@Param("id") Long id);
}
