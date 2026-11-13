package com.logicore.operations;

import com.logicore.audit.AuditService;
import com.logicore.auth.Access;
import com.logicore.customers.*;
import com.logicore.users.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperationService {

  private final OperationRepository repo;
  private final OperationHistoryRepository history;
  private final UserAccountRepository users;
  private final PartnerRepository partners;
  private final Access access;
  private final AuditService audit;

  public OperationService(
    OperationRepository r,
    OperationHistoryRepository h,
    UserAccountRepository u,
    PartnerRepository p,
    Access a,
    AuditService log
  ) {
    repo = r;
    history = h;
    users = u;
    partners = p;
    access = a;
    audit = log;
  }

  public record Input(
    @NotBlank @Size(max = 80) String reference,
    @NotBlank @Size(max = 60) String type,
    @NotNull Long customerId,
    @NotBlank @Size(max = 160) String origin,
    @NotBlank @Size(max = 160) String destination,
    @NotNull LocalDate estimatedDate,
    @NotNull Long assignedId,
    @Size(max = 4000) String description,
    long version
  ) {}

  public static final Map<String, List<String>> NEXT = Map.of(
    "DRAFT",
    List.of("PENDING", "CANCELLED"),
    "PENDING",
    List.of("IN_PROGRESS", "CANCELLED"),
    "IN_PROGRESS",
    List.of("IN_TRANSIT", "CANCELLED"),
    "IN_TRANSIT",
    List.of("DELIVERED", "CANCELLED"),
    "DELIVERED",
    List.of("COMPLETED"),
    "COMPLETED",
    List.of(),
    "CANCELLED",
    List.of()
  );

  public Operation get(Long id) {
    var o = repo
      .findById(id)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Operation not found."
        )
      );
    access.operation(o);
    return o;
  }

  @Transactional
  public Operation save(Long id, Input in) {
    var me = access.user();
    Operation o =
      id == null
        ? new Operation()
        : repo
            .lock(id)
            .orElseThrow(() ->
              new ResponseStatusException(HttpStatus.NOT_FOUND)
            );
    if (id != null) {
      access.operation(o);
      if (o.version != in.version()) throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Operation changed. Refresh first."
      );
      if (
        List.of("COMPLETED", "CANCELLED").contains(o.status)
      ) throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "This operation is closed."
      );
    }
    if (
      me.role.equals("OPERATOR") && !me.id.equals(in.assignedId())
    ) Access.deny();
    var assignee = users
      .findById(in.assignedId())
      .filter(u -> u.active)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Choose an active operator."
        )
      );
    partners
      .findById(in.customerId())
      .filter(p -> p.active && p.kind.equals("CUSTOMER"))
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Choose an active customer."
        )
      );
    o.reference = in.reference().trim();
    o.type = in.type();
    o.customerId = in.customerId();
    o.origin = in.origin();
    o.destination = in.destination();
    o.estimatedDate = in.estimatedDate();
    o.assignedId = assignee.id;
    o.description = in.description();
    if (id == null) o.createdBy = me.id;
    repo.saveAndFlush(o);
    record(o, id == null ? "CREATED" : "UPDATED", "Operation details saved");
    return o;
  }

  @Transactional
  public Operation status(Long id, String next) {
    var o = repo
      .lock(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    access.operation(o);
    if (
      !NEXT.getOrDefault(o.status, List.of()).contains(next)
    ) throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "Invalid status transition."
    );
    if (
      access.user().role.equals("OPERATOR") && next.equals("CANCELLED")
    ) Access.deny();
    String previous = o.status;
    o.status = next;
    if (next.equals("COMPLETED")) o.completedAt = Instant.now();
    record(o, "STATUS", previous + " → " + next);
    return repo.save(o);
  }

  private void record(Operation o, String event, String detail) {
    var h = new OperationHistory();
    h.operationId = o.id;
    h.actorId = access.user().id;
    h.event = event;
    h.detail = detail;
    history.save(h);
    audit.record(event, "OPERATION", o.id, detail);
  }
}
