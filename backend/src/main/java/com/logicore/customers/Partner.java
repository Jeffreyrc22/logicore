package com.logicore.customers;

import com.logicore.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "partner")
public class Partner extends BaseEntity {

  public String kind;
  public String name;
  public String company;
  public String email;
  public String phone;
  public String address;
  public String category;
  public boolean active = true;
}
