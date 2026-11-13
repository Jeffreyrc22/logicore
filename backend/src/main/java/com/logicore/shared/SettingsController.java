package com.logicore.shared;

import com.logicore.audit.AuditService;
import com.logicore.auth.Access;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/settings")
public class SettingsController {

  private final CompanySettingRepository repo;
  private final Access access;
  private final AuditService audit;

  public SettingsController(
    CompanySettingRepository r,
    Access a,
    AuditService l
  ) {
    repo = r;
    access = a;
    audit = l;
  }

  record Input(
    @NotBlank @Size(max = 160) String companyName,
    @NotBlank @Size(max = 100) String timezone,
    @Email @Size(max = 254) String supportEmail
  ) {}

  @GetMapping
  public CompanySetting get() {
    access.admin();
    return repo
      .findAll()
      .stream()
      .findFirst()
      .orElseGet(() -> {
        var s = new CompanySetting();
        s.companyName = "LogiCore";
        s.timezone = "America/Bogota";
        s.supportEmail = "";
        return s;
      });
  }

  @PutMapping
  @Transactional
  public CompanySetting save(@Valid @RequestBody Input in) {
    access.admin();
    java.time.ZoneId.of(in.timezone());
    var s = get();
    s.companyName = in.companyName();
    s.timezone = in.timezone();
    s.supportEmail = in.supportEmail();
    repo.saveAndFlush(s);
    audit.record("UPDATED", "SETTINGS", s.id, "Company preferences saved");
    return s;
  }
}
