package com.logicore.reports;

import com.logicore.auth.Access;
import com.logicore.inventory.InventoryItemRepository;
import java.time.LocalDate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

  private final ReportService service;
  private final Access access;
  private final InventoryItemRepository inventory;

  public ReportController(
    ReportService s,
    Access a,
    InventoryItemRepository i
  ) {
    service = s;
    access = a;
    inventory = i;
  }

  @GetMapping("/dashboard")
  public Object dashboard() {
    return service.dashboard(null, null);
  }

  @GetMapping("/reports/operations")
  public Object operations(
    @RequestParam(required = false) LocalDate from,
    @RequestParam(required = false) LocalDate to
  ) {
    access.manager();
    return service.dashboard(from, to);
  }

  @GetMapping("/reports/inventory")
  public Object inventory() {
    access.manager();
    return inventory.findAll();
  }

  @GetMapping("/reports/operations.csv")
  public ResponseEntity<String> csv(
    @RequestParam(required = false) LocalDate from,
    @RequestParam(required = false) LocalDate to
  ) {
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
      .header("Content-Disposition", "attachment; filename=operations.csv")
      .body(service.csv(from, to));
  }
}
