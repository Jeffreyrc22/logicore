package com.logicore.shared;

import com.logicore.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "company_setting")
public class CompanySetting extends BaseEntity {

  public String companyName;
  public String timezone;
  public String supportEmail;
}
