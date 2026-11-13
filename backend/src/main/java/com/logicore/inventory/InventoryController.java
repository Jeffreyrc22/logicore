package com.logicore.inventory;

import com.logicore.auth.Access;
import com.logicore.shared.Queries;
import jakarta.validation.Valid;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

  private final InventoryItemRepository repo;
  private final InventoryMovementRepository movements;
  private final InventoryService service;
  private final Access access;

  public InventoryController(
    InventoryItemRepository r,
    InventoryMovementRepository m,
    InventoryService s,
    Access a
  ) {
    repo = r;
    movements = m;
    service = s;
    access = a;
  }

  @GetMapping
  public Object list(
    @RequestParam(defaultValue = "") String q,
    @RequestParam(defaultValue = "false") boolean low,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "12") int size
  ) {
    access.manager();
    Specification<InventoryItem> spec = Queries.search(
      q,
      "sku",
      "name",
      "category"
    );
    if (low) spec = spec.and((r, c, b) ->
      b.lessThanOrEqualTo(r.get("quantity"), r.get("minimumStock"))
    );
    return repo.findAll(spec, Queries.page(page, size));
  }

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public InventoryItem create(@Valid @RequestBody InventoryService.Input in) {
    return service.save(null, in);
  }

  @PutMapping("/{id}")
  public InventoryItem edit(
    @PathVariable Long id,
    @Valid @RequestBody InventoryService.Input in
  ) {
    return service.save(id, in);
  }

  @PostMapping("/{id}/movements")
  public InventoryItem move(
    @PathVariable Long id,
    @Valid @RequestBody InventoryService.Movement in
  ) {
    return service.move(id, in);
  }

  @GetMapping("/{id}/movements")
  public Object movements(@PathVariable Long id) {
    access.manager();
    return movements
      .findAll(Queries.eq("itemId", id), Queries.page(0, 100))
      .getContent();
  }
}
