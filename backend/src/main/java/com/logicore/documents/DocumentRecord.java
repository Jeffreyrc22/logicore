package com.logicore.documents;

import com.logicore.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "document_record")
public class DocumentRecord extends BaseEntity {

  public String filename;
  public String type;
  public String description;
  public Long operationId;
  public Long uploadedBy;
  public long size;
  public String contentType;
  public String status = "ACTIVE";

  @com.fasterxml.jackson.annotation.JsonIgnore
  @Transient
  public byte[] content;
}
