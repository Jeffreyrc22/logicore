package com.logicore.users;

import com.logicore.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "user_account")
public class UserAccount extends BaseEntity {

  @Column(unique = true, nullable = false)
  public String email;

  @com.fasterxml.jackson.annotation.JsonIgnore
  public String passwordHash;

  public String name;
  public String role = "OPERATOR";
  public boolean active = true;
  public java.time.Instant lastLogin;
}
