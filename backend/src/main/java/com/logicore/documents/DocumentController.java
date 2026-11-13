package com.logicore.documents;

import com.logicore.auth.Access;
import com.logicore.operations.Operation;
import com.logicore.shared.Queries;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

  private final DocumentRecordRepository repo;
  private final DocumentService service;
  private final Access access;

  public DocumentController(
    DocumentRecordRepository r,
    DocumentService s,
    Access a
  ) {
    repo = r;
    service = s;
    access = a;
  }

  @GetMapping
  public Object list(
    @RequestParam(defaultValue = "") String q,
    @RequestParam(defaultValue = "") String type,
    @RequestParam(required = false) Long operationId,
    @RequestParam(defaultValue = "") String status,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "12") int size
  ) {
    var me = access.user();
    Specification<DocumentRecord> spec = Queries.<DocumentRecord>search(
      q,
      "filename",
      "description"
    )
      .and(Queries.eq("type", type))
      .and(Queries.eq("status", status))
      .and(Queries.eq("operationId", operationId));
    if (me.role.equals("OPERATOR")) spec = spec.and((r, c, b) -> {
      var sub = c.subquery(Long.class);
      var op = sub.from(Operation.class);
      sub.select(op.get("id")).where(b.equal(op.get("assignedId"), me.id));
      return r.get("operationId").in(sub);
    });
    return repo.findAll(spec, Queries.page(page, size));
  }

  @GetMapping("/{id}")
  public DocumentRecord get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DocumentRecord upload(
    @RequestParam Long operationId,
    @RequestParam String type,
    @RequestParam(defaultValue = "") String description,
    @RequestParam MultipartFile file
  ) throws java.io.IOException {
    return service.upload(operationId, type, description, file);
  }

  record Metadata(
    @NotBlank String type,
    @NotNull @Size(max = 2000) String description,
    @NotBlank String status,
    long version
  ) {}

  @PutMapping("/{id}")
  public DocumentRecord edit(
    @PathVariable Long id,
    @Valid @RequestBody Metadata in
  ) {
    return service.update(
      id,
      in.type(),
      in.description(),
      in.status(),
      in.version()
    );
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<byte[]> download(@PathVariable Long id) {
    var d = service.get(id);
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(d.contentType))
      .header(
        "Content-Disposition",
        ContentDisposition.attachment()
          .filename(d.filename, java.nio.charset.StandardCharsets.UTF_8)
          .build()
          .toString()
      )
      .header("X-Content-Type-Options", "nosniff")
      .header("Cache-Control", "no-store")
      .body(service.download(id));
  }
}
