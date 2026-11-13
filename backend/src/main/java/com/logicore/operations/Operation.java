package com.logicore.operations;

import com.logicore.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "operation")
public class Operation extends BaseEntity {

  @Column(unique = true)
  public String reference;

  public String type;
  public Long customerId;
  public String origin;
  public String destination;
  public java.time.LocalDate estimatedDate;
  public java.time.Instant completedAt;
  public String status = "DRAFT";
  public Long assignedId;
  public String description;
  public Long createdBy;
}
