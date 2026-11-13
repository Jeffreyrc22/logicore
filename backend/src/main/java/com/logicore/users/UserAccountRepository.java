package com.logicore.users;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository
  extends
    JpaRepository<UserAccount, Long>,
    JpaSpecificationExecutor<UserAccount>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from UserAccount e where e.id=:id")
  Optional<UserAccount> lock(@Param("id") Long id);

  Optional<UserAccount> findByEmailIgnoreCase(String email);
  long countByRoleAndActiveTrue(String role);
}
