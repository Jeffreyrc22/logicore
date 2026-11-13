package com.logicore.documents;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseDocumentStorage implements DocumentStorage {

  private final JdbcTemplate jdbc;

  public DatabaseDocumentStorage(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public byte[] read(DocumentRecord record) {
    return jdbc.queryForObject(
      "SELECT content FROM document_content WHERE document_id=?",
      byte[].class,
      record.id
    );
  }

  public void write(DocumentRecord record, byte[] content) {
    jdbc.update(
      "INSERT INTO document_content(document_id,content) VALUES(?,?)",
      record.id,
      content
    );
  }
}
