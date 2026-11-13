package com.logicore.audit;

import com.logicore.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {

  public Long actorId;
  public String action;
  public String entity;
  public Long entityId;
  public String detail;
}
