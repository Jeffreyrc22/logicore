package com.logicore.shared;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CompanySettingRepository
  extends
    JpaRepository<CompanySetting, Long>,
    JpaSpecificationExecutor<CompanySetting>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from CompanySetting e where e.id=:id")
  Optional<CompanySetting> lock(@Param("id") Long id);
}
