package com.logicore.customers;

import com.logicore.auth.Access;
import com.logicore.shared.Queries;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PartnerController {

  private final PartnerRepository repo;
  private final PartnerService service;
  private final Access access;

  public PartnerController(PartnerRepository r, PartnerService s, Access a) {
    repo = r;
    service = s;
    access = a;
  }

  private String kind(String kind) {
    return kind.equals("customers") ? "CUSTOMER" : "SUPPLIER";
  }

  @GetMapping("/{kind:customers|suppliers}")
  public Object list(
    @PathVariable String kind,
    @RequestParam(defaultValue = "") String q,
    @RequestParam(required = false) Boolean active,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "12") int size
  ) {
    access.user();
    return repo.findAll(
      Queries.<Partner>eq("kind", kind(kind))
        .and(Queries.search(q, "name", "company", "email"))
        .and(Queries.eq("active", active)),
      Queries.page(page, size)
    );
  }

  @PostMapping("/{kind:customers|suppliers}")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Partner create(
    @PathVariable String kind,
    @Valid @RequestBody PartnerService.Input in
  ) {
    return service.save(kind(kind), null, in);
  }

  @PutMapping("/{kind:customers|suppliers}/{id}")
  public Partner edit(
    @PathVariable String kind,
    @PathVariable Long id,
    @Valid @RequestBody PartnerService.Input in
  ) {
    return service.save(kind(kind), id, in);
  }
}
