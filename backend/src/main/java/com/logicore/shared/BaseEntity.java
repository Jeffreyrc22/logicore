package com.logicore.shared;

import jakarta.persistence.*;
import java.time.Instant;

@MappedSuperclass
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Version
  public long version;

  public Instant createdAt = Instant.now();
  public Instant updatedAt = Instant.now();

  @PreUpdate
  public void touch() {
    updatedAt = Instant.now();
  }
}
