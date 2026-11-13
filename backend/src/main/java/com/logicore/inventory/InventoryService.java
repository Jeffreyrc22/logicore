package com.logicore.inventory;

import com.logicore.audit.AuditService;
import com.logicore.auth.Access;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {

  private final InventoryItemRepository repo;
  private final InventoryMovementRepository movements;
  private final Access access;
  private final AuditService audit;

  public InventoryService(
    InventoryItemRepository r,
    InventoryMovementRepository m,
    Access a,
    AuditService l
  ) {
    repo = r;
    movements = m;
    access = a;
    audit = l;
  }

  public record Input(
    @NotBlank @Size(max = 80) String sku,
    @NotBlank @Size(max = 160) String name,
    @Size(max = 4000) String description,
    @NotBlank @Size(max = 100) String category,
    @Min(0) @Max(1000000) int minimumStock,
    @NotBlank @Size(max = 30) String unit,
    @NotBlank @Size(max = 160) String location,
    boolean active,
    long version
  ) {}

  public record Movement(
    @Min(-1000000) @Max(1000000) int delta,
    @NotBlank @Size(max = 500) String reason
  ) {}

  @Transactional
  public InventoryItem save(Long id, Input in) {
    access.manager();
    var item =
      id == null
        ? new InventoryItem()
        : repo
            .lock(id)
            .orElseThrow(() ->
              new ResponseStatusException(HttpStatus.NOT_FOUND)
            );
    if (
      id != null && item.version != in.version()
    ) throw new ResponseStatusException(HttpStatus.CONFLICT);
    item.sku = in.sku();
    item.name = in.name();
    item.description = in.description();
    item.category = in.category();
    item.minimumStock = in.minimumStock();
    item.unit = in.unit();
    item.location = in.location();
    item.active = in.active();
    repo.saveAndFlush(item);
    audit.record(
      id == null ? "CREATED" : "UPDATED",
      "INVENTORY",
      item.id,
      "Item metadata saved"
    );
    return item;
  }

  @Transactional
  public InventoryItem move(Long id, Movement in) {
    access.manager();
    var item = repo
      .lock(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    long balance = (long) item.quantity + in.delta();
    if (
      !item.active || in.delta() == 0 || balance < 0 || balance > 1000000
    ) throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "Movement would exceed available stock or limits."
    );
    item.quantity = (int) balance;
    var m = new InventoryMovement();
    m.itemId = id;
    m.actorId = access.user().id;
    m.delta = in.delta();
    m.reason = in.reason();
    m.balance = item.quantity;
    movements.save(m);
    audit.record(
      "STOCK_MOVEMENT",
      "INVENTORY",
      id,
      "Delta " + in.delta() + ", balance " + balance
    );
    return repo.save(item);
  }
}
