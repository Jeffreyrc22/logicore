package com.logicore.inventory;

import com.logicore.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "inventory_movement")
public class InventoryMovement extends BaseEntity {

  public Long itemId;
  public Long actorId;
  public int delta;
  public String reason;
  public int balance;
}
