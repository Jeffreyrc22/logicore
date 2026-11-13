package com.logicore.documents;

public interface DocumentStorage {
  byte[] read(DocumentRecord record);
  void write(DocumentRecord record, byte[] content);
}
