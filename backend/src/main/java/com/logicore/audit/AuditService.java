package com.logicore.audit;

import com.logicore.auth.Access;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

  private final AuditLogRepository repo;
  private final Access access;

  public AuditService(AuditLogRepository r, Access a) {
    repo = r;
    access = a;
  }

  public void record(String action, String entity, Long id, String detail) {
    AuditLog l = new AuditLog();
    l.actorId = access.user().id;
    l.action = action;
    l.entity = entity;
    l.entityId = id;
    l.detail = detail;
    repo.save(l);
  }
}
