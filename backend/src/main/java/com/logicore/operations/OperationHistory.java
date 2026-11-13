package com.logicore.operations;

import com.logicore.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "operation_history")
public class OperationHistory extends BaseEntity {

  public Long operationId;
  public Long actorId;
  public String event;
  public String detail;
}
