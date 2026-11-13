package com.logicore.reports;

import com.logicore.audit.*;
import com.logicore.auth.Access;
import com.logicore.documents.*;
import com.logicore.inventory.*;
import com.logicore.operations.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

  private final OperationRepository operations;
  private final InventoryItemRepository inventory;
  private final DocumentRecordRepository documents;
  private final OperationHistoryRepository history;
  private final Access access;

  public ReportService(
    OperationRepository o,
    InventoryItemRepository i,
    DocumentRecordRepository d,
    OperationHistoryRepository h,
    Access a
  ) {
    operations = o;
    inventory = i;
    documents = d;
    history = h;
    access = a;
  }

  public List<Operation> operations(LocalDate from, LocalDate to) {
    var me = access.user();
    return operations
      .findAll()
      .stream()
      .filter(o -> !me.role.equals("OPERATOR") || me.id.equals(o.assignedId))
      .filter(
        o ->
          from == null ||
          !o.createdAt.atZone(ZoneOffset.UTC).toLocalDate().isBefore(from)
      )
      .filter(
        o ->
          to == null ||
          !o.createdAt.atZone(ZoneOffset.UTC).toLocalDate().isAfter(to)
      )
      .toList();
  }

  public Map<String, Object> dashboard(LocalDate from, LocalDate to) {
    var me = access.user();
    var ops = operations(from, to);
    var ids = ops
      .stream()
      .map(o -> o.id)
      .collect(Collectors.toSet());
    var docs = documents
      .findAll()
      .stream()
      .filter(d -> ids.contains(d.operationId) && d.status.equals("ACTIVE"))
      .toList();
    var items = me.role.equals("OPERATOR")
      ? List.<InventoryItem>of()
      : inventory.findAll();
    var stats = new LinkedHashMap<String, Object>();
    stats.put("totalOperations", ops.size());
    stats.put(
      "activeShipments",
      ops
        .stream()
        .filter(o ->
          Set.of("PENDING", "IN_PROGRESS", "IN_TRANSIT").contains(o.status)
        )
        .count()
    );
    stats.put(
      "pendingDocuments",
      ops
        .stream()
        .filter(
          o ->
            !Set.of("CANCELLED", "COMPLETED").contains(o.status) &&
            docs.stream().noneMatch(d -> d.operationId.equals(o.id))
        )
        .count()
    );
    stats.put(
      "completedDeliveries",
      ops
        .stream()
        .filter(o -> Set.of("DELIVERED", "COMPLETED").contains(o.status))
        .count()
    );
    stats.put(
      "inventoryAlerts",
      items
        .stream()
        .filter(i -> i.active && i.quantity <= i.minimumStock)
        .count()
    );
    stats.put(
      "monthlyOperations",
      ops
        .stream()
        .filter(o ->
          YearMonth.from(o.createdAt.atZone(ZoneOffset.UTC)).equals(
            YearMonth.now(ZoneOffset.UTC)
          )
        )
        .count()
    );
    stats.put(
      "byStatus",
      ops
        .stream()
        .collect(
          Collectors.groupingBy(
            o -> o.status,
            TreeMap::new,
            Collectors.counting()
          )
        )
    );
    stats.put(
      "byMonth",
      ops
        .stream()
        .collect(
          Collectors.groupingBy(
            o -> YearMonth.from(o.createdAt.atZone(ZoneOffset.UTC)).toString(),
            TreeMap::new,
            Collectors.counting()
          )
        )
    );
    stats.put(
      "inventoryByCategory",
      items
        .stream()
        .collect(
          Collectors.groupingBy(
            i -> i.category,
            TreeMap::new,
            Collectors.summingInt(i -> i.quantity)
          )
        )
    );
    stats.put(
      "documentSummary",
      docs
        .stream()
        .collect(
          Collectors.groupingBy(
            d -> d.type,
            TreeMap::new,
            Collectors.counting()
          )
        )
    );
    stats.put(
      "recent",
      history
        .findAll()
        .stream()
        .filter(h -> ids.contains(h.operationId))
        .sorted(
          Comparator.comparing((OperationHistory h) -> h.createdAt).reversed()
        )
        .limit(8)
        .toList()
    );
    return stats;
  }

  public String csv(LocalDate from, LocalDate to) {
    access.manager();
    StringBuilder b = new StringBuilder(
      "Reference,Type,Origin,Destination,Status,Estimated date\r\n"
    );
    for (var o : operations(from, to)) {
      b.append(
        String.join(
          ",",
          cell(o.reference),
          cell(o.type),
          cell(o.origin),
          cell(o.destination),
          cell(o.status),
          cell(String.valueOf(o.estimatedDate))
        )
      ).append("\r\n");
    }
    return b.toString();
  }

  public static String cell(String v) {
    if (v == null) return "\"\"";
    if (v.matches("^[=+@\\-\\t\\r].*")) v = "'" + v;
    return "\"" + v.replace("\"", "\"\"") + "\"";
  }
}
