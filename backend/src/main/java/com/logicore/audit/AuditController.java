package com.logicore.audit;

import com.logicore.auth.Access;
import com.logicore.shared.Queries;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit")
public class AuditController {

  private final AuditLogRepository repo;
  private final Access access;

  public AuditController(AuditLogRepository r, Access a) {
    repo = r;
    access = a;
  }

  @GetMapping
  public Object list(
    @RequestParam(defaultValue = "") String q,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "12") int size
  ) {
    access.admin();
    return repo.findAll(
      Queries.search(q, "action", "entity", "detail"),
      Queries.page(page, size)
    );
  }
}
