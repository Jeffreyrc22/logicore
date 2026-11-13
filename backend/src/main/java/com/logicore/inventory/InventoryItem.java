package com.logicore.inventory;

import com.logicore.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "inventory_item")
public class InventoryItem extends BaseEntity {

  @Column(unique = true)
  public String sku;

  public String name;
  public String description;
  public String category;
  public int quantity;
  public int minimumStock;
  public String unit;
  public String location;
  public boolean active = true;
}
