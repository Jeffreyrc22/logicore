package com.logicore.operations;

import com.logicore.auth.Access;
import com.logicore.shared.Queries;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/operations")
public class OperationController {

  private final OperationRepository repo;
  private final OperationService service;
  private final OperationHistoryRepository history;
  private final Access access;

  public OperationController(
    OperationRepository r,
    OperationService s,
    OperationHistoryRepository h,
    Access a
  ) {
    repo = r;
    service = s;
    history = h;
    access = a;
  }

  @GetMapping
  public Page<Operation> list(
    @RequestParam(defaultValue = "") String q,
    @RequestParam(defaultValue = "") String status,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "12") int size
  ) {
    var me = access.user();
    Specification<Operation> spec = Queries.<Operation>search(
      q,
      "reference",
      "origin",
      "destination"
    ).and(Queries.eq("status", status));
    if (me.role.equals("OPERATOR")) spec = spec.and(
      Queries.eq("assignedId", me.id)
    );
    return repo.findAll(spec, Queries.page(page, size));
  }

  @GetMapping("/{id}")
  public Operation get(@PathVariable Long id) {
    return service.get(id);
  }

  @GetMapping("/{id}/history")
  public Object history(@PathVariable Long id) {
    service.get(id);
    return history
      .findAll(Queries.eq("operationId", id), Queries.page(0, 100))
      .getContent();
  }

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Operation create(@Valid @RequestBody OperationService.Input in) {
    return service.save(null, in);
  }

  @PutMapping("/{id}")
  public Operation edit(
    @PathVariable Long id,
    @Valid @RequestBody OperationService.Input in
  ) {
    return service.save(id, in);
  }

  record Status(@jakarta.validation.constraints.NotBlank String status) {}

  @PatchMapping("/{id}/status")
  public Operation status(
    @PathVariable Long id,
    @Valid @RequestBody Status in
  ) {
    return service.status(id, in.status());
  }
}
