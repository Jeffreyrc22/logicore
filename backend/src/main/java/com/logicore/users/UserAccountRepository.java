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

  @Modifying(clearAutomatically = true)
  @org.springframework.transaction.annotation.Transactional
  @Query("update UserAccount u set u.lastLogin=:time, u.version=u.version+1 where u.id=:id")
  void recordLogin(@Param("id") Long id, @Param("time") java.time.Instant time);

  Optional<UserAccount> findByEmailIgnoreCase(String email);
  long countByRoleAndActiveTrue(String role);
}

