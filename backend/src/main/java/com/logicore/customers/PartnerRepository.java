package com.logicore.customers;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PartnerRepository
  extends JpaRepository<Partner, Long>, JpaSpecificationExecutor<Partner>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Partner e where e.id=:id")
  Optional<Partner> lock(@Param("id") Long id);
}
