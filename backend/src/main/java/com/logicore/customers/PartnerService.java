package com.logicore.customers;

import com.logicore.audit.AuditService;
import com.logicore.auth.Access;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PartnerService {

  private final PartnerRepository repo;
  private final Access access;
  private final AuditService audit;

  public PartnerService(PartnerRepository r, Access a, AuditService l) {
    repo = r;
    access = a;
    audit = l;
  }

  public record Input(
    @NotBlank @Size(max = 160) String name,
    @Size(max = 160) String company,
    @Email @Size(max = 254) String email,
    @Size(max = 60) String phone,
    @Size(max = 500) String address,
    @Size(max = 100) String category,
    boolean active,
    long version
  ) {}

  @Transactional
  public Partner save(String kind, Long id, Input in) {
    access.manager();
    var p =
      id == null
        ? new Partner()
        : repo
            .lock(id)
            .orElseThrow(() ->
              new ResponseStatusException(HttpStatus.NOT_FOUND)
            );
    if (
      id != null && (!p.kind.equals(kind) || p.version != in.version())
    ) throw new ResponseStatusException(HttpStatus.CONFLICT);
    p.kind = kind;
    p.name = in.name();
    p.company = in.company();
    p.email = in.email();
    p.phone = in.phone();
    p.address = in.address();
    p.category = in.category();
    p.active = in.active();
    repo.saveAndFlush(p);
    audit.record(
      id == null ? "CREATED" : "UPDATED",
      kind,
      p.id,
      "Contact details updated"
    );
    return p;
  }
}
