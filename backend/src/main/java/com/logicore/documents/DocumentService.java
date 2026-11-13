package com.logicore.documents;

import com.logicore.audit.AuditService;
import com.logicore.auth.Access;
import com.logicore.operations.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentService {

  private final DocumentRecordRepository repo;
  private final OperationService operations;
  private final Access access;
  private final AuditService audit;
  private final DocumentStorage storage;

  public DocumentService(
    DocumentRecordRepository r,
    OperationService o,
    Access a,
    AuditService l,
    DocumentStorage s
  ) {
    repo = r;
    operations = o;
    access = a;
    audit = l;
    storage = s;
  }

  public static final Set<String> TYPES = Set.of(
    "INVOICE",
    "SHIPPING_DOCUMENT",
    "DELIVERY_RECEIPT",
    "INVENTORY_REPORT",
    "CONTRACT",
    "OTHER"
  );

  public DocumentRecord get(Long id) {
    var d = repo
      .findById(id)
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found.")
      );
    operations.get(d.operationId);
    return d;
  }

  @Transactional
  public DocumentRecord upload(
    Long operationId,
    String type,
    String description,
    MultipartFile file
  ) throws IOException {
    operations.get(operationId);
    if (
      !TYPES.contains(type) ||
      description.length() > 2000 ||
      file.isEmpty() ||
      file.getSize() > 10485760
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Check document type, description and file size."
    );
    String name = Optional.ofNullable(file.getOriginalFilename()).orElse(
      "document"
    );
    if (
      name.length() > 180 ||
      name.contains("/") ||
      name.contains("\\") ||
      name.chars().anyMatch(c -> c < 32)
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Invalid filename."
    );
    byte[] content = file.getBytes();
    String lower = name.toLowerCase(),
      mime;
    if (lower.endsWith(".pdf")) {
      String start = new String(
        content,
        0,
        Math.min(8, content.length),
        StandardCharsets.US_ASCII
      );
      String tail = new String(
        content,
        Math.max(0, content.length - 2048),
        Math.min(2048, content.length),
        StandardCharsets.US_ASCII
      );
      if (
        !start.startsWith("%PDF-") || !tail.contains("%%EOF")
      ) throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Invalid PDF content."
      );
      mime = "application/pdf";
    } else if (
      lower.endsWith(".png") ||
      lower.endsWith(".jpg") ||
      lower.endsWith(".jpeg")
    ) {
      try (
        var stream = ImageIO.createImageInputStream(
          new ByteArrayInputStream(content)
        )
      ) {
        var readers = ImageIO.getImageReaders(stream);
        if (!readers.hasNext()) throw new IllegalArgumentException();
        var reader = readers.next();
        try {
          reader.setInput(stream);
          if (
            (long) reader.getWidth(0) * reader.getHeight(0) > 16000000
          ) throw new IllegalArgumentException();
          var image = reader.read(0);
          var out = new ByteArrayOutputStream();
          boolean png = lower.endsWith(".png");
          ImageIO.write(image, png ? "png" : "jpg", out);
          content = out.toByteArray();
          mime = png ? "image/png" : "image/jpeg";
        } finally {
          reader.dispose();
        }
      }
    } else if (lower.endsWith(".txt")) {
      try {
        StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .decode(java.nio.ByteBuffer.wrap(content));
      } catch (CharacterCodingException e) {
        throw new IllegalArgumentException();
      }
      for (byte b : content) if (b == 0) throw new IllegalArgumentException();
      mime = "text/plain";
    } else throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Use PDF, PNG, JPEG or UTF-8 TXT."
    );
    var d = new DocumentRecord();
    d.filename = name;
    d.type = type;
    d.description = description;
    d.operationId = operationId;
    d.uploadedBy = access.user().id;
    d.size = content.length;
    d.contentType = mime;
    repo.saveAndFlush(d);
    storage.write(d, content);
    audit.record("UPLOADED", "DOCUMENT", d.id, "Operation " + operationId);
    return d;
  }

  @Transactional
  public DocumentRecord update(
    Long id,
    String type,
    String description,
    String status,
    long version
  ) {
    var d = get(id);
    if (d.version != version) throw new ResponseStatusException(
      HttpStatus.CONFLICT
    );
    if (
      !TYPES.contains(type) ||
      description.length() > 2000 ||
      !Set.of("ACTIVE", "ARCHIVED").contains(status)
    ) throw new IllegalArgumentException();
    if (!status.equals(d.status)) access.manager();
    d.type = type;
    d.description = description;
    d.status = status;
    audit.record("UPDATED", "DOCUMENT", d.id, "Status " + status);
    return repo.save(d);
  }

  @Transactional
  public byte[] download(Long id) {
    var d = get(id);
    audit.record("DOWNLOADED", "DOCUMENT", id, "Authorized download");
    return storage.read(d);
  }
}
